package common.communication;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class for the communications between client-side to server-side Includes
 * all the elements required to transmit a valid message that is Serializable
 */
public class Communication implements Serializable {
	private static final long serialVersionUID = 1L;
	private String uniqueId; // will hold a unique id for client-server identification

	////////////////////
	/// TABLES NAMES ///
	////////////////////

	// tables in context of a specific park
	public static final String activeBookings = "_park_active_booking";
	public static final String cancelledBookings = "_park_cancelled_booking";
	public static final String doneBookings = "_park_done_booking";
	public static final String parkEmployees = "_park_employees";
	public static final String waitingList = "_park_waiting_list";
	// tables in context of stakeholders
	public static final String departmentManager = "department_manager";
	public static final String parkManager = "park_manager";
	public static final String traveller = "traveller";
	public static final String griupGuide = "group_guide";
	public static final String representative = "representative";
	public static final String systemUser = "system_users";
	// tables in context of management
	public static final String invoice = "invoice";
	public static final String park = "park";
	public static final String pendingAdjustment = "pending_adjustment";
	public static final String viewedAdjustment = "viewed_adjustment";
	public static final String pricing = "pricing";
	// reports tables
	public static final String totalReport = "total_number_of_visitors_report";
	public static final String usageReport = "usage_report";

	/// CNCELLATION REASONS ///
	public static final String userCancelled = "User Cancelled";
	public static final String userDidNotArrive = "Did not arrive";
	public static final String userDidNotConfirm = "Did not confirm";

	// the communication type
	/**
	 * QUERY_REQUEST: a client-server message for executing a single query
	 * 
	 * TRANSACTION: a client-server message for multiple queries
	 * 
	 * CLIENT_SERVER_MESSAGE: a client-server message that is not a query
	 * 
	 * SERVER_CLIENT_MESSAGE: a server-client message
	 * 
	 * SELF: an inner-server query request
	 */
	public enum CommunicationType {
		QUERY_REQUEST, TRANSACTION, CLIENT_SERVER_MESSAGE, SERVER_CLIENT_MESSAGE, SELF;
	}

	private CommunicationType communicationType; // the communication type

	/**
	 * Constructor of a Communication object
	 * 
	 * @param communicationType the type of the communication
	 */
	public Communication(CommunicationType communicationType) { // Constructor
		uniqueId = UUID.randomUUID().toString();
		this.communicationType = communicationType;
		if (communicationType == CommunicationType.QUERY_REQUEST
				|| communicationType == CommunicationType.TRANSACTION) { // if it's a query request
			clientMessageType = ClientMessageType.NONE;
			setServerMessageType(ServerMessageType.NONE);

		} else if (communicationType == CommunicationType.CLIENT_SERVER_MESSAGE) { // if it's a client-server message
			queryType = QueryType.NONE;
			setServerMessageType(ServerMessageType.NONE);

		} else if (communicationType == CommunicationType.SERVER_CLIENT_MESSAGE) { // if it's a message from server-side
																					// to client-side
			clientMessageType = ClientMessageType.NONE;
			queryType = QueryType.NONE;

		} else { // if this is a self-server message
			clientMessageType = ClientMessageType.NONE;
			setServerMessageType(ServerMessageType.NONE);
		}
	}

	/////////////////////////////////////////////////////////////////////
	/// SQL QUERY AND TRANSACTION COMMUNICATION REQUESTS - PROPERTIES ///
	/////////////////////////////////////////////////////////////////////

	// determines the type of the SQL requested query
	public enum QueryType {
		SELECT, UPDATE, INSERT, DELETE, NONE;
	}

	private QueryType queryType;

	// determines the table/s query is going to work on
	private ArrayList<String> tables;

	// for SELECT query, determines the selected columns (can also be '*')
	private ArrayList<String> selectColumns;

	// determines the "where" part conditions
	private ArrayList<String> whereColumns;
	private ArrayList<Object> whereValues;
	private ArrayList<String> whereOperators;

	// determines the columns and values for "set" and "insert"
	private ArrayList<String> columns;
	private ArrayList<Object> values;

	// a list of communications to execute as a transaction
	private ArrayList<Communication> requestsList = null;

	// set to true if the query is done in a critical section
	private boolean isCritical; // for critical sections park capacities updates

	/////////////////////////////////////////////////////////
	/// CLIENT-SERVER MESSAGES COMMUNICATION - PROPERTIES ///
	/////////////////////////////////////////////////////////

	public enum ClientMessageType {
		CONNECT, DISCONNECT, NONE;
	}

	private ClientMessageType clientMessageType;

	/////////////////////////////////////////////////////////
	/// SERVER-CLIENT MESSAGES COMMUNICATION - PROPERTIES ///
	/////////////////////////////////////////////////////////

	public enum ServerMessageType {
		RESPONSE, CONQUER, NONE;
	}

	private ServerMessageType serverMessageType;

	private String serverMessageContent;
	private ArrayList<Object[]> resultList; // a container for the result set from the database, as ArrayList
	private boolean queryResult; // holds the result of update/insert/delete queries

	////////////////////////////////////////////////////
	/// SECONDARY REQUEST COMMUNICATION - PROPERTIES ///
	////////////////////////////////////////////////////

	public enum SecondaryRequest {
		SEND_CONFIRMATION, SEND_CANCELLATION, UPDATE_WAITING_LIST, SEND_CONFIRMATION_WITHOUT_REMINDER, UPDATE_CAPACITY;
	}

	private SecondaryRequest secondaryRequest;
	private int parkId;
	private LocalDate date;
	private LocalTime time;
	private String parkName, email, phone, fullName, parkLocation;
	private int visitors, price;
	private boolean paid;

	///////////////////////
	/// GENERAL METHODS ///
	///////////////////////

	/**
	 * @return the communication type
	 */
	public CommunicationType getCommunicationType() {
		return communicationType;
	}

	/**
	 * @return the unique Communication id
	 */
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * This method sets the unique id of the Communication object
	 * 
	 * @param uniqueId the unique Communication id
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	///////////////////////////////////////////////////////////
	/// METHODS FOR HANDLING QUERY AND TRANSACTION REQUESTS ///
	///////////////////////////////////////////////////////////

	///////////////
	/// GETTERS ///
	///////////////

	/**
	 * This method returns the QueryType enum of the communication
	 * 
	 * @return the query type (an enum constant)
	 */
	public QueryType getQueryType() {
		return queryType;
	}

	/**
	 * This method returns the secondery request if exists
	 * 
	 * @return the secondary request, null if does not exist
	 */
	public SecondaryRequest getSecondaryRequest() {
		return secondaryRequest;
	}

	/**
	 * This method returns the secondery request's park id
	 * 
	 * @return the park id
	 */
	public int getParkId() {
		return parkId;
	}

	/**
	 * This method returns the secondery request's date
	 * 
	 * @return the date
	 */
	public LocalDate getDate() {
		return date;
	}

	/**
	 * This method returns the secondery request's time
	 * 
	 * @return the time
	 */
	public LocalTime getTime() {
		return time;
	}

	/**
	 * This method returns the secondary request's park name
	 * 
	 * @return the park name
	 */
	public String getParkName() {
		return parkName;
	}

	/**
	 * This method returns the secondery request's email address
	 * 
	 * @return the email address
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * This method returns the secondery request's phone number
	 * 
	 * @return the phone number
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * This method returns the secondery request's full name
	 * 
	 * @return the full name
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * This method returns the secondery request's visitors number
	 * 
	 * @return the visitors number
	 */
	public int getVisitors() {
		return visitors;
	}

	/**
	 * This method returns the secondery request's final price
	 * 
	 * @return the final price
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * This method returns the secondery request's paid property
	 * 
	 * @return true if paid, false if not
	 */
	public boolean isPaid() {
		return paid;
	}

	/**
	 * This method returns the secondery request's park location
	 * 
	 * @return parkLocation
	 */
	public String getParkLocation() {
		return parkLocation;
	}

	/**
	 * This method returns if this query is in a critical section
	 * 
	 * @return the isCritical property
	 */
	public boolean isCritical() {
		return isCritical;
	}

	///////////////
	/// SETTERS ///
	///////////////

	/**
	 * Determines the type of the SQL query requested
	 * 
	 * @param queryType the type of the SQL query requested
	 * @throws CommunicationException if trying to set a QueryType where the
	 *                                CommunicationType is not a QUERY_REQUEST
	 */
	public void setQueryType(QueryType queryType) throws CommunicationException {
		if (!(communicationType == CommunicationType.QUERY_REQUEST || communicationType == CommunicationType.SELF)) {
			throw new CommunicationException("The communication type is not a query request");
		}
		this.queryType = queryType;
	}

	/**
	 * Sets the tables the SQL query will execute on
	 * 
	 * @param tables the tables the query will execute on
	 */
	public void setTables(List<String> tables) {
		this.tables = new ArrayList<String>(tables);
	}

	/**
	 * If the query is SELECT, this method sets the columns in the returened view
	 * from the database
	 * 
	 * @param selectColumns a List of Strings denoting the SELECT columns
	 */
	public void setSelectColumns(List<String> selectColumns) {
		this.selectColumns = new ArrayList<String>(selectColumns);
	}

	/**
	 * This method sets the WHERE part of the query. Limitations:
	 * whereColumns.size() must be equal to whereValues.size(), Moreover,
	 * whereOperators.size() must be equal to whereColumns.size() +
	 * whereValues.size() - 1
	 * 
	 * @param whereColumns   a List of Strings denoting the WHERE columns
	 * @param whereOperators a List of Strings denoting the WHERE operators ("=",
	 *                       ">", "<", ">=", "<=", "!=", "AND", "OR", etc.)
	 * @param whereValues    a List of Objects denoting the values to check of each
	 *                       columns
	 */
	public void setWhereConditions(List<String> whereColumns, List<String> whereOperators, List<Object> whereValues) {
		this.whereColumns = new ArrayList<String>(whereColumns);
		this.whereOperators = new ArrayList<String>(whereOperators);
		this.whereValues = new ArrayList<Object>(whereValues);
	}

	/**
	 * This method sets the columns and values (of the INSERT and SET parts)
	 * 
	 * @param columns a List of String denoting the columns of SET or INSERT
	 * @param values  a List of Objects denoting the columns' values
	 */
	public void setColumnsAndValues(List<String> columns, List<Object> values) {
		this.columns = new ArrayList<String>(columns);
		this.values = new ArrayList<Object>(values);
	}

	/**
	 * Thus method sets the secondary request, if relevant
	 * 
	 * @param secondaryRequest
	 */
	public void setSecondaryRequest(SecondaryRequest secondaryRequest) {
		this.secondaryRequest = secondaryRequest;
	}

	/**
	 * This method sets the secondary request's park id
	 * 
	 * @param parkId
	 */
	public void setParkId(int parkId) {
		this.parkId = parkId;
	}

	/**
	 * This method sets the secondary request's date
	 * 
	 * @param date
	 */
	public void setDate(LocalDate date) {
		this.date = date;
	}

	/**
	 * This method sets the secondary request's time
	 * 
	 * @param time
	 */
	public void setTime(LocalTime time) {
		this.time = time;
	}

	/**
	 * This method sets the secondary request's park name
	 * 
	 * @param parkName
	 */
	public void setParkName(String parkName) {
		this.parkName = parkName;
	}

	/**
	 * This method sets the secondary request's email address
	 * 
	 * @param email
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * This method sets the secondary request's phone number
	 * 
	 * @param phone
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * This method sets the secondary request's full name
	 * 
	 * @param fullName
	 */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/**
	 * This method sets the secondary request's visitors number
	 * 
	 * @param visitor
	 */
	public void setVisitors(int visitors) {
		this.visitors = visitors;
	}

	/**
	 * This method sets the secondary request's final price
	 * 
	 * @param price
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * This method sets the secondary request's paid property
	 * 
	 * @param paid
	 */
	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	/**
	 * This method sets the secondary request's park location
	 * 
	 * @param parkLocation
	 */
	public void setParkLocation(String parkLocation) {
		this.parkLocation = parkLocation;
	}

	/**
	 * This method sets the critical section boolean property
	 * 
	 * @param isCritical
	 */
	public void setCritical(boolean isCritical) {
		this.isCritical = isCritical;
	}

	/////////////////////////////////
	/// QUERY COMBINATION METHODS ///
	/////////////////////////////////

	/**
	 * This method redirects the query creation to the specific method according to
	 * the query type
	 * 
	 * @return a String ready for execution in the DatabaseController
	 * @throws CommunicationException if trying to combine a query while the
	 *                                Communication is not of QUERY_REQUEST type
	 */
	public String combineQuery() throws CommunicationException {
		switch (queryType) {
		case SELECT:
			return combineSelectQuery();
		case UPDATE:
			return combineUpdateQuery();
		case INSERT:
			return combineInsertQuery();
		case DELETE:
			return combineDeleteQuery();
		default: // NONE
			throw new CommunicationException("No query type chosen");
		}
	}

	/**
	 * This method creates and returns a SELECT query
	 * 
	 * @return a String of a SELECT SQL query
	 * @throws CommunicationException if the tables or selectColumns are not
	 *                                included in the QUERY_REQUEST Communication
	 */
	private String combineSelectQuery() throws CommunicationException {
		String query = "SELECT ";
		// adding the column/s to select from
		if (selectColumns == null)
			throw new CommunicationException("Columns are not included");
		if (selectColumns.size() == 1) { // only one column, or *
			query += selectColumns.get(0) + " ";
		} else { // several columns from the table/s
			for (int i = 0; i < selectColumns.size(); i++) {
				if (i + 1 == selectColumns.size()) { // if this is the last column, no ',' after it
					query += selectColumns.get(i) + " ";
				} else {
					query += selectColumns.get(i) + ",";
				}
			}
		}

		// adding the tables to select from
		query += "FROM ";
		if (tables == null)
			throw new CommunicationException("Tables are not included");
		if (tables.size() == 1) { // only one column, or *
			query += tables.get(0) + " ";
		} else { // several columns from the table/s
			for (int i = 0; i < tables.size(); i++) {
				if (i + 1 == tables.size()) { // if this is the last column, no ',' after it
					query += tables.get(i) + "";
				} else {
					query += tables.get(i) + ",";
				}
			}
		}

		// adding the where part
		query += createWherePart() + ";";

		return query;
	}

	/**
	 * This method creates and returns a UPDATE query
	 * 
	 * @return a String of a UPDATE SQL query
	 * @throws CommunicationException if the tables or columns or values are not
	 *                                included in the QUERY_REQUEST Communication
	 */
	private String combineUpdateQuery() throws CommunicationException {
		String query = "UPDATE ";
		// adding the table name
		if (tables == null)
			throw new CommunicationException("Tables are not included");
		query += tables.get(0) + " ";

		// adding the column/s to set values to
		query += "SET ";
		if (columns == null) {
			throw new CommunicationException("Columns are not included");
		}
		if (values == null) {
			throw new CommunicationException("Values are not included");
		}
		if (values.size() != columns.size()) {
			throw new CommunicationException("Columns or values are missing");
		}

		for (int i = 0; i < columns.size(); i++) {
			query += columns.get(i) + " = ";
			query += prepareValue(values.get(i)) + (i + 1 == columns.size() ? "" : ", ");
		}

		// adding the where part
		query += createWherePart() + ";";
		return query;
	}

	/**
	 * This method creates and returns a INSERT query
	 * 
	 * @return a String of a INSERT SQL query
	 * @throws CommunicationException if the tables or columns or values are not
	 *                                included in the QUERY_REQUEST Communication
	 */
	private String combineInsertQuery() throws CommunicationException {
		int i;
		String query = "INSERT INTO ";
		// adding the table name
		if (tables == null)
			throw new CommunicationException("Table is not included");
		query += tables.get(0) + " (";

		// adding the columns
		if (columns == null) {
			throw new CommunicationException("Columns are not included");
		}
		for (i = 0; i < columns.size(); i++) {
			query += columns.get(i) + ((i + 1 == columns.size()) ? ") " : ",");
		}

		// adding the values
		if (values == null) {
			throw new CommunicationException("Values are not included");
		}
		if (values.size() != columns.size()) {
			throw new CommunicationException("Columns and values are not matching");
		}
		query += "VALUES (";
		for (i = 0; i < values.size(); i++) {
			query += prepareValue(values.get(i)) + ((i + 1 == values.size()) ? ")" : ",");
		}

		query += ";";
		return query;
	}

	/**
	 * This method creates and returns a DELETE query
	 * 
	 * @return a String of a DELETE SQL query
	 * @throws CommunicationException if the tables are not included in the
	 *                                QUERY_REQUEST Communication
	 */
	private String combineDeleteQuery() throws CommunicationException {
		String query = "DELETE FROM ";
		// adding the table name
		if (tables == null)
			throw new CommunicationException("Table is not included");
		query += tables.get(0) + "";

		// adding the where part
		query += createWherePart() + ";";
		return query;
	}

	/**
	 * This method gets a value object and prepares it to fit the SQL syntax
	 * 
	 * @param value to prepare for the SQL syntax
	 * @return the prepared value
	 */
	private String prepareValue(Object value) {
		if (value == null)
			return null;
		else {
			String ret = "";
			if (value instanceof LocalTime) {
				ret += "'" + (((LocalTime) value).getHour() < 10 ? "0" : "");
				ret += ((LocalTime) value).getHour() + ":";
				ret += (((LocalTime) value).getMinute() < 10 ? "0" : "");
				ret += ((LocalTime) value).getMinute() + ":00'";
				return ret;
			} else if (value instanceof LocalDate) {
				ret += "'" + ((LocalDate) value).getYear() + "-";
				ret += (((LocalDate) value).getMonthValue() < 10 ? "0" : "");
				ret += ((LocalDate) value).getMonthValue() + "-";
				ret += (((LocalDate) value).getDayOfMonth() < 10 ? "0" : "");
				ret += ((LocalDate) value).getDayOfMonth() + "'";
				return ret;
			} else if (value instanceof Number) {
				return ((Number) value).toString();
			} else if (value instanceof String
					&& (((String) value).matches("[a-zA-Z]+\\s[-+]\\s\\d+") || ((String) value).charAt(0) == '(')) {
				return (String) value;
			} else {
				return "'" + value.toString() + "'";
			}
		}
	}

	/**
	 * This method creates, if relevant, the WHERE part of the query, as string
	 * 
	 * @return a String of the WHERE part of the query
	 * @throws CommunicationException if the amount of values is not equal to the
	 *                                amount of columns, or if the amount of
	 *                                operators does not fit the amount of columns
	 *                                and values
	 */
	private String createWherePart() throws CommunicationException {
		String where = "";
		if (whereColumns != null && whereOperators != null && whereValues != null) {
			if (whereValues.size() != whereColumns.size()
					|| whereOperators.size() != whereValues.size() + whereColumns.size() - 1) {
				throw new CommunicationException("Columns and values are not matching");
			}
			where += " WHERE ";
			int j = 0;
			for (int i = 0; i < whereColumns.size(); i++) {
				where += whereColumns.get(i) + " " + whereOperators.get(j++) + " ";
				where += prepareValue(whereValues.get(i)) + (j < whereOperators.size() ? " " : "");
				where += j < whereOperators.size() ? whereOperators.get(j++) + " " : "";
			}
		}
		return where;
	}

	/**
	 * This method is called in order to add a request to a list of requests to be
	 * executed as a transaction
	 * 
	 * @param request
	 */
	public void addRequestToList(Communication request) {
		if (requestsList == null) {
			requestsList = new ArrayList<Communication>();
		}
		requestsList.add(request);
	}

	/**
	 * This method returns the requests list of a transaction communication
	 * 
	 * @return the requests list
	 */
	public ArrayList<Communication> getRequestsList() {
		if (getCommunicationType() == CommunicationType.TRANSACTION) {
			return requestsList;
		}
		return null;
	}

	////////////////////////////////////////////////////////////
	/// METHODS FOR HANDLING CLIENT TO SERVER COMMUNICATIONS ///
	////////////////////////////////////////////////////////////

	///////////////
	/// GETTERS ///
	///////////////

	/**
	 * @return the MessageType, if the Communication is from CLIENT_SERVER_MESSAGE
	 *         type
	 */
	public ClientMessageType getClientMessageType() {
		return clientMessageType;
	}

	///////////////
	/// SETTERS ///
	///////////////

	/**
	 * This method sets the MessageType
	 * 
	 * @param messageType the MessageType to be set
	 */
	public void setClientMessageType(ClientMessageType clientMessageType) {
		this.clientMessageType = clientMessageType;
	}

	////////////////////////////////////////////////////////////
	/// METHODS FOR HANDLING SERVER TO CLIENT COMMUNICATIONS ///
	////////////////////////////////////////////////////////////

	///////////////
	/// GETTERS ///
	///////////////

	/**
	 * This method returns the result list (to use on the client-side), after it was
	 * inserted by the server-side. The result list is an ArrayList of Object[],
	 * denoting the view returned from a SELECT query execution.
	 * 
	 * @return the result list
	 */
	public ArrayList<Object[]> getResultList() {
		return resultList;
	}

	/**
	 * This method returns the query result (true = succeed, false = failed), after
	 * it was inserted by the server-side. The result is a boolean variablem
	 * denoting the result of UPDATE/INSERT/DELETE queries.
	 * 
	 * @return the query result
	 */
	public boolean getQueryResult() {
		return queryResult;
	}

	/**
	 * @return the server-client type of message
	 */
	public ServerMessageType getServerMessageType() {
		return serverMessageType;
	}

	/**
	 * @return the server message content
	 */
	public String getServerMessageContent() {
		return serverMessageContent;
	}

	/**
	 * This method returns the result of the transaction
	 * 
	 * @return true if all queries succeed, false otherwise
	 */
	public boolean getTransactionResult() {
		for (Communication request : requestsList) {
			if (!request.getQueryResult())
				return false;
		}
		return true;
	}

	///////////////
	/// SETTERS ///
	///////////////

	/**
	 * This method sets the result list to a Communication of RESPONSE type, by the
	 * server-side to the later use of the client-side.
	 * 
	 * @param resultList the result list of a SELECT query
	 */
	public void setResultList(ArrayList<Object[]> resultList) {
		this.resultList = resultList;
	}

	/**
	 * This method sets the query result to a Communication of RESPONSE type, by the
	 * server-side to the later use of the client-side.
	 * 
	 * @param queryResult the query result of an UPDATE/INSERT/DELETE queries
	 */
	public void setQueryResult(boolean queryResult) {
		this.queryResult = queryResult;
	}

	/**
	 * Sets the server-client type of message
	 * 
	 * @param serverMessageType
	 */
	public void setServerMessageType(ServerMessageType serverMessageType) {
		this.serverMessageType = serverMessageType;
	}

	/**
	 * Sets the server message content
	 * 
	 * @param serverMessageContent
	 */
	public void setServerMessageContent(String serverMessageContent) {
		this.serverMessageContent = serverMessageContent;
	}
}