package edu.gatech.sqltutor.beans;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import edu.gatech.sqltutor.DatabaseManager;
import edu.gatech.sqltutor.DatabaseTable;
import edu.gatech.sqltutor.QueryResult;
import edu.gatech.sqltutor.QuestionTuple;
import edu.gatech.sqltutor.TestConst;
import edu.gatech.sqltutor.rules.er.ERDiagram;
import edu.gatech.sqltutor.rules.er.ERSerializer;
import edu.gatech.sqltutor.rules.er.mapping.ERMapping;
import edu.gatech.sqltutor.rules.lang.SymbolicFragmentTranslator;

@ManagedBean
@ViewScoped
public class TutorialPageBean extends AbstractDatabaseBean implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	private String selectedSchema;
	private List<DatabaseTable> tables;
	private List<String> questions = new ArrayList<String>();
	private HashMap<String, String> answers = new HashMap<String, String>();
	private int questionIndex;
	private String query;
	private String feedbackNLP;
	private String resultSetFeedback;
	private String userFeedback;
	private QueryResult queryResult;
	private QueryResult answerResult;
	private QueryResult queryDiffResult;
	private QueryResult answerDiffResult;
	private boolean nlpDisabled = true;

	@PostConstruct
	public void init() {
		selectedSchema = userBean.getSelectedSchema();
		try {
			tables = getDatabaseManager().getTables(selectedSchema);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		setQuestionsAndAnswers();
	}

	public void processSQL() {
		reset();
		
		if ( StringUtils.isEmpty(query) || questions.isEmpty() )
			return;
		
		try {
			// check the answer
			queryResult = getDatabaseManager().getQueryResult(selectedSchema, query, userBean.isAdmin());
			setResultSetDiffs();
			
			// generate NLP feedback
			ERDiagram erDiagram = null;
			ERMapping erMapping = null;
			if(userBean.getSelectedSchema().equals("company")) {
				nlpDisabled = false;
				Class<?> c = this.getClass();
				ERSerializer serializer = new ERSerializer();
				erDiagram = (ERDiagram)serializer.deserialize(c.getResourceAsStream(TestConst.Resources.COMPANY_DIAGRAM));
				erMapping = (ERMapping)serializer.deserialize(c.getResourceAsStream(TestConst.Resources.COMPANY_MAPPING));
			} else if(userBean.getSelectedSchema().equals("business_trip")) {
				nlpDisabled = false;
				Class<?> c = this.getClass();
				ERSerializer serializer = new ERSerializer();
				erDiagram = (ERDiagram)serializer.deserialize(c.getResourceAsStream(TestConst.Resources.BUSINESS_TRIP_DIAGRAM));
				erMapping = (ERMapping)serializer.deserialize(c.getResourceAsStream(TestConst.Resources.BUSINESS_TRIP_MAPPING));
			}

			if(!nlpDisabled && getQueryIsCorrect() == false) {
				try {
					SymbolicFragmentTranslator queryTranslator = new SymbolicFragmentTranslator();
					queryTranslator.setQuery(query);
					queryTranslator.setSchemaMetaData(tables);
					queryTranslator.setERDiagram(erDiagram);
					queryTranslator.setERMapping(erMapping); 
					String result = queryTranslator.getTranslation();
					result = format(result);
					resultSetFeedback += " We determined the question that you actually answered was: ";
					feedbackNLP = "\" " + result + " \"";
				} catch (Exception e) {
					e.printStackTrace();
					resultSetFeedback += " (Sorry, we were unable to produce a sound English translation for your query.)";
					// FIXME: log the error
				}
			}
		} catch(SQLException e) {
			resultSetFeedback = "Incorrect. Your query was malformed. Please try again.\n" + e.getMessage();
		}
		
		try {
			String nlpFeedback = StringUtils.isEmpty(getFeedbackNLP()) ? null : getFeedbackNLP();
			getDatabaseManager().log(getSessionId(), userBean.getHashedEmail(), selectedSchema, 
					questions.get(questionIndex), getAnswers().get(questions.get(questionIndex)), query, !isQueryMalformed(), getQueryIsCorrect(), nlpFeedback);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	} 
	
	private String format(String translation) {
		Pattern pattern = Pattern.compile("(_.*?_)");
		Matcher matcher = pattern.matcher(translation);
		while(matcher.find()) {
		    String oldString = matcher.group(1);
		    String newString = oldString.replaceFirst("_", "<i>");
		    newString = newString.replaceFirst("_", "</i>");
		    translation = translation.replace(oldString, newString);
		}
		return translation;
	}
	
	private String getIpAddress() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress == null) {
		    ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}
	
	private String getSessionId() {
		FacesContext fCtx = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fCtx.getExternalContext().getSession(false);
		return session.getId();
	}
	
	private void setResultSetDiffs() {
		try {
			answerResult = getDatabaseManager().getQueryResult(selectedSchema, getAnswers().get(questions.get(questionIndex)), userBean.isAdmin());
			queryDiffResult = new QueryResult(queryResult);
			queryDiffResult.getColumns().removeAll(answerResult.getColumns());
			queryDiffResult.getData().removeAll(answerResult.getData());
			answerDiffResult = new QueryResult(answerResult);
			answerDiffResult.getColumns().removeAll(queryResult.getColumns());
			answerDiffResult.getData().removeAll(queryResult.getData());
			
			if (getAnswers().get(questions.get(questionIndex)).toLowerCase().contains(" order by ")) {
				compareQueries(false, true); 
			} else {
				compareQueries(false, false); 
			} 
			
		} catch(SQLException e) {
			resultSetFeedback = "The stored answer was malformed." + e.getMessage();
		}
	}
	
	private void compareQueries(boolean columnOrderMatters, boolean rowOrderMatters) {
		
		if(!queryResult.getColumns().containsAll(answerResult.getColumns()) || !answerResult.getColumns().containsAll(queryResult.getColumns())) {
			resultSetFeedback = "Incorrect.";
		} else {
			if(columnOrderMatters && rowOrderMatters) {
				if(answerResult.equals(queryResult)) {
					resultSetFeedback = "Correct!";
				} else {
					resultSetFeedback = "Incorrect. Your query's data differed from the stored answer's.";
					//FIXME perhaps more specific feedback? different row data/order, order by? conditionals? different attributes?
				}
			} else if(columnOrderMatters && !rowOrderMatters) {
				String queryDiffAnswer = query + " EXCEPT " + getAnswers().get(questions.get(questionIndex)) + ";";
				String answerDiffQuery = getAnswers().get(questions.get(questionIndex)) + " EXCEPT " + query + ";";
				try {
					final DatabaseManager databaseManager = getDatabaseManager();
					queryDiffResult = databaseManager.getQueryResult(selectedSchema, queryDiffAnswer, userBean.isAdmin());
					answerDiffResult = databaseManager.getQueryResult(selectedSchema, answerDiffQuery, userBean.isAdmin());
					if(queryDiffResult.getData().isEmpty() && answerDiffResult.getData().isEmpty()) {
						resultSetFeedback = "Correct.";
					} else {
						resultSetFeedback = "Incorrect. Your query's data differed from the stored answer's.";			
					}
				} catch(SQLException e) {
					if(e.getMessage().contains("columns")) {
						resultSetFeedback = "Incorrect. The number of columns in your result did not match the answer.";
					} else if(e.getMessage().contains("type")) {
						resultSetFeedback = "Incorrect. One or more of your result's data types did not match the answer.";
					} 
				}
			} else if(!columnOrderMatters && rowOrderMatters) {
				Map<String, List<String>> queryTree = new TreeMap<String, List<String>>();
				Map<String, List<String>> answerTree = new TreeMap<String, List<String>>();
				for(int i = 0; i < queryResult.getColumns().size(); i++) {
					List<String> columnData = new ArrayList<String>();
					for(int j = 0; j < queryResult.getData().size(); j++) {
						columnData.add(queryResult.getData().get(j).get(i));
					}
					queryTree.put(queryResult.getColumns().get(i), columnData);
				}
				for(int i = 0; i < answerResult.getColumns().size(); i++) {
					List<String> columnData = new ArrayList<String>();
					for(int j = 0; j < answerResult.getData().size(); j++) {
						columnData.add(answerResult.getData().get(j).get(i));
					}
					answerTree.put(answerResult.getColumns().get(i), columnData);
				}
				if(queryTree.equals(answerTree)) {
					resultSetFeedback = "Correct.";
				} else {
					resultSetFeedback = "Incorrect. Your query's data or order differed from the stored answer's.";
				}
			} else {
				Multiset<String> queryBag = HashMultiset.create();
				Multiset<String> answerBag = HashMultiset.create();
				for(int i = 0; i < queryResult.getColumns().size(); i++) {
					for(int j = 0; j < queryResult.getData().size(); j++) {
						queryBag.add(queryResult.getData().get(j).get(i));
					}
				}
				for(int i = 0; i < answerResult.getColumns().size(); i++) {
					for(int j = 0; j < answerResult.getData().size(); j++) {
						answerBag.add(answerResult.getData().get(j).get(i));
					}
				}
				if(queryBag.equals(answerBag)) {
					resultSetFeedback = "Correct.";
				} else {
					resultSetFeedback = "Incorrect. Your query's data differed from the stored answer's.";
				} 
			}
		}
	}
	
	public void submitFeedback() {
		// FIXME We'll need to decide how we're going to store this.
		String feedbackMessagesId = FacesContext.getCurrentInstance().getViewRoot().findComponent(":feedbackForm:feedbackMessages").getClientId();
		FacesContext.getCurrentInstance().addMessage(feedbackMessagesId, new FacesMessage("We appreciate your submission."));
	}
	
	public void setQuestionsAndAnswers() {
		List<QuestionTuple> questionTuples = null;
		final DatabaseManager databaseManager = getDatabaseManager();
		try {
			questionTuples = databaseManager.getQuestions(selectedSchema);
		} catch (SQLException e) {
			e.getNextException().printStackTrace();
		}
		
		if(questionTuples.isEmpty()) {
			questions.add("There are no questions available for this schema.");
		} else {
			HashMap<String, Boolean> options = null;
			try {
				options = databaseManager.getOptions(selectedSchema);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			for(QuestionTuple question : questionTuples) {
				questions.add(question.getQuestion());
				getAnswers().put(question.getQuestion(), question.getAnswer());
			}
			
			if(!options.get("in_order_questions")) {
				Collections.shuffle(questions, new Random(System.nanoTime()));
			}
			
		}
	}
	
	public void nextQuestion() {	// reset everything and move to the next question.
		questionIndex++;
		if(questionIndex >= questions.size()) {
			questionIndex = 0;
		}
		reset();
	}
	
	private void reset() {
		queryResult = null;
		answerResult = null;
		queryDiffResult = null;
		answerDiffResult = null;
		feedbackNLP = "";
		resultSetFeedback = "";
		nlpDisabled = true;
	}
	
	public String getQuestion() {
		return questions.get(questionIndex);
	}

	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}

	public QueryResult getQueryResult() {
		return queryResult;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}

	public String getFeedbackNLP() {
		return feedbackNLP;
	}
	
	public String getSelectedSchema() {
		return selectedSchema;
	}

	public List<DatabaseTable> getTables() {
		return tables;
	}

	public QueryResult getAnswerResult() {
		return answerResult;
	}

	public QueryResult getQueryDiffResult() {
		return queryDiffResult;
	}
	
	public QueryResult getAnswerDiffResult() {
		return answerDiffResult;
	}
	
	public String getResultSetFeedback() {
		return resultSetFeedback;
	}
	
	public boolean getQueryIsCorrect() {
		if(resultSetFeedback == null || !resultSetFeedback.toLowerCase().contains("incorrect")) {
			return true;
		}
		return false;
	}

	public void setUserFeedback(String userFeedback) {
		this.userFeedback = userFeedback;
	}

	public String getUserFeedback() {
		return userFeedback;
	}
	
	public boolean isNlpDisabled() {
		return nlpDisabled;
	}
	
	public boolean isQueryMalformed() {
		if(resultSetFeedback.toLowerCase().contains("malformed")) {
			return true;
		}
		return false;
	}

	public HashMap<String, String> getAnswers() {
		return answers;
	}

	public void setAnswers(HashMap<String, String> answers) {
		this.answers = answers;
	}
}
