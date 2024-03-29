package clientSide.gui;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import clientSide.control.GoNatureUsersController;
import clientSide.control.ParkController;
import common.communication.Communication;
import common.controllers.AbstractScreen;
import common.controllers.ScreenException;
import common.controllers.ScreenManager;
import common.controllers.StatefulException;
import entities.Booking;
import entities.ParkEmployee;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/**
 * Controls the functionality related to managing park entries and exits from
 * the perspective of a park employee. It provides mechanisms for park employees
 * to update entry and exit times of bookings, based on the visitors' actual
 * arrival and departure, and to issue invoices for park services.
 *
 * This controller leverages `ParkController` for interactions with the
 * park-related data and `GoNatureUsersController` for session management. It
 * supports validating booking IDs against current park bookings, updating the
 * park's current capacity, and navigating to invoice generation or payment
 * processing screens as needed.
 * 
 */
public class ParkEntryManagementScreenController extends AbstractScreen {

	private ParkController parkControl;
	private GoNatureUsersController userControl;
	private ParkEmployee parkEmployee;
	private Booking booking;

	/**
	 * Constructor, initializes the Park Controller instance
	 */
	public ParkEntryManagementScreenController() {
		parkControl = ParkController.getInstance();
		userControl = GoNatureUsersController.getInstance();
	}

	//////////////////////////////////
	/// JAVAFX AND FXML COMPONENTS ///
	//////////////////////////////////

	@FXML
	private Button backButton, entryTimeBtn, exitTimeBtn, invoiceBtn, checkBtn;
	@FXML
	private TextField bookingIDTxt;
	@FXML
	private ImageView goNatureLogo;
	@FXML
	private Label titleLbl, bookingIDLbl, fullNameLbl, dateLbl, sizeLbl, priceLbl, isPaidLbl, arrivalLbl, leavingLbl,
			statusLbl;
	@FXML
	private Pane pane;

	//////////////////////////////
	/// EVENT HANDLING METHODS ///
	//////////////////////////////

	@FXML
	void checkIdClicked(ActionEvent event) {
		// reset lables
		fullNameLbl.setText("");
		dateLbl.setText("");
		sizeLbl.setText("");
		priceLbl.setText("");
		isPaidLbl.setText("");
		arrivalLbl.setText("");
		leavingLbl.setText("");
		statusLbl.setText("");

		if (!validate())
			return;
		booking = findBookingAcrossTables(bookingIDTxt.getText());
		if (booking == null) {
			showErrorAlert("No existing reservations found for the provided booking ID.");
			bookingIDTxt.setStyle(setFieldToError());
			return;
		} else {
			booking.setParkBooked(parkEmployee.getWorkingIn());
		}

		fullNameLbl.setText(booking.getFirstName() + " " + booking.getLastName());
		dateLbl.setText(booking.getDayOfVisit() + " " + booking.getTimeOfVisit());
		sizeLbl.setText(booking.getNumberOfVisitors() + "");
		priceLbl.setText(booking.getFinalPrice() == -1 ? "N/A" : booking.getFinalPrice() + "$");
		isPaidLbl.setText(booking.isPaid() ? "Yes" : "No");
		arrivalLbl.setText(booking.getEntryParkTime() == null ? "N/A" : booking.getEntryParkTime().toString());
		leavingLbl.setText(booking.getExitParkTime() == null ? "N/A" : booking.getExitParkTime().toString());

		if (booking.getFinalPrice() == -1) { // means the booking is cancelled
			statusLbl.setText("Cancelled booking");
			entryTimeBtn.setDisable(true);
			exitTimeBtn.setDisable(true);
			invoiceBtn.setDisable(true);
		}

		else { // means the booking is active/done
				// it's an active booking, before entering the park
			if (booking.getEntryParkTime() == null && booking.getExitParkTime() == null) {
				statusLbl.setText("Active booking");
				entryTimeBtn.setDisable(false);
				if (booking.isPaid()) { // if paid, can generate an invoice now
					invoiceBtn.setDisable(false);
				} else {
					invoiceBtn.setDisable(true);
				}
				exitTimeBtn.setDisable(true);
			}

			// it's an active booking, after entering the park
			if (booking.getEntryParkTime() != null && booking.getExitParkTime() == null) {
				statusLbl.setText("Active booking");
				entryTimeBtn.setDisable(true);
				exitTimeBtn.setDisable(false);
				invoiceBtn.setDisable(false);
			} else { // it's a done booking
				statusLbl.setText("Done booking");
				invoiceBtn.setDisable(false);
			}
		}
	}

	/**
	 * Handles the event triggered by pressing the 'Invoice production' button. It
	 * attempts to retrieve a booking from the database using the provided booking
	 * ID. If successful, navigates the park employee to the Payment System Screen
	 * for processing the payment or generating an invoice for the booking. Displays
	 * an error alert if the booking ID does not correspond to an existing
	 * reservation in the park.
	 *
	 * @param event The ActionEvent triggered by pressing the 'Invoice production'
	 *              button.
	 */
	@FXML
	void invoiceScreen(ActionEvent event) {
		try {
			ScreenManager.getInstance().showScreen("ConfirmationScreenController",
					"/clientSide/fxml/ConfirmationScreen.fxml", true, false, booking);
		} catch (StatefulException | ScreenException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Updates the entry time for a visitor based on a valid booking ID. It first
	 * validates the booking ID, then checks for the booking's existence and its
	 * eligibility for entry at the current time. If all conditions are met, the
	 * entry time is updated in the database, and the park's current capacity is
	 * adjusted accordingly. This method also handles payment verification.
	 *
	 * @param event The ActionEvent triggered by the employee's interaction with the
	 *              UI to update a booking's entry time.
	 */
	@FXML
	void updateEntryTime(ActionEvent event) {
		String bookingId = bookingIDTxt.getText();
		String parkTable = parkControl.nameOfTable(parkEmployee.getWorkingIn());

		LocalDateTime visitTime = LocalDateTime.of(booking.getDayOfVisit(), booking.getTimeOfVisit());
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(visitTime)) {
			// Checking if the visitor did not arrive earlier than his reservation
			if (!parkControl.checkParkAvailabilityForBooking(booking)) {
				showErrorAlert("The visitor arrived too early.\nPlease inform the visitor they can't enter now. "
						+ "\nSuggest they come closer to their reservation time or check back later. ");
				return;
			} else {
				showInformationAlert("The visitor arrived earlier than the scheduled time, but there is space available in"
						+ " the park! They can enter now and enjoy their visit.!");
			}
		}

		updateParkCapacity();
		// updates entry time
		parkControl.updateTimeInPark(parkTable + Communication.activeBookings, "entryParkTime", bookingId);
		int updateCapacity = parkEmployee.getWorkingIn().getCurrentCapacity() + booking.getNumberOfVisitors();
		parkEmployee.getWorkingIn().setCurrentCapacity(updateCapacity);

		// updates park's current capacity
		if (parkControl.updateCurrentCapacity(parkEmployee.getWorkingIn().getParkName(), updateCapacity))
			if (!booking.isPaid()) {// needs to update DB: "paid" ?
				if (parkControl.payForBooking(parkTable)) //
					System.out.println("Payment successful.");
				int decision = showConfirmationAlert(
						"Please charge the customer:\nFinal reservation's price is: " + booking.getFinalPrice() + "$",
						Arrays.asList("By Cash", "By Credit Card"));
				if (decision == 2) {// if the user clicked on "Credit Card" he will redirect to pay screen and then
									// to confirmation screen
					event.consume();
					try {
						booking.setPaid(true);
						ScreenManager.getInstance().showScreen("PaymentSystemScreenController",
								"/clientSide/fxml/PaymentSystemScreen.fxml", true, false, booking);
					} catch (StatefulException | ScreenException e) {
						e.printStackTrace();
					}
				} else { // user clicked on "Cash", showing the confirmation screen
					try {
						ScreenManager.getInstance().showScreen("ConfirmationScreenController",
								"/clientSide/fxml/ConfirmationScreen.fxml", true, false, booking);
					} catch (StatefulException | ScreenException e1) {
						e1.printStackTrace();
					}
				}
			} else { // already paid
				arrivalLbl.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
			}
	}

	/**
	 * Updates the exit time for a visitor based on a valid booking ID. This method
	 * validates the booking ID and confirms the booking's existence. It ensures
	 * that an entry time has been set for the booking, indicating that the visitor
	 * did enter the park. If the booking is for the current day and an entry time
	 * exists without a corresponding exit time, the exit time is updated in the
	 * database. The park's current capacity is then adjusted to reflect the
	 * visitor's departure. Additionally, the booking is moved from the active
	 * bookings table to the done bookings table, finalizing the visitor's park
	 * attendance record.
	 *
	 * @param event The ActionEvent triggered by the employee's interaction with the
	 *              UI to update a booking's exit time.
	 */
	@FXML
	void updateExitTime(ActionEvent event) {
		String bookingId = bookingIDTxt.getText();
		String parkTable = parkControl.nameOfTable(parkEmployee.getWorkingIn());

		updateParkCapacity();
		parkControl.updateTimeInPark(parkTable + Communication.activeBookings, "exitParkTime", bookingId); // updates
																											// exit time
		int updateCapacity = parkEmployee.getWorkingIn().getCurrentCapacity() - booking.getNumberOfVisitors();

		parkControl.updateCurrentCapacity(parkEmployee.getWorkingIn().getParkName(), updateCapacity);// updates park
																										// current
																										// capacity
		// remove the booking from active park table
		parkControl.removeBookingFromActiveBookings(parkTable + Communication.activeBookings, booking.getBookingId());
		// insert the booking to done park table
		parkControl.insertBookingToTable(booking, parkTable + Communication.doneBookings, "done");
		exitTimeBtn.setDisable(true);
		leavingLbl.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
		statusLbl.setText("Done booking");
	}

	@FXML
	/**
	 * Returning the user the the previous screen
	 * 
	 * @param event
	 */
	void returnToPreviousScreen(ActionEvent event) {
		try {
			ScreenManager.getInstance().goToPreviousScreen(false, false);
		} catch (ScreenException | StatefulException e) {
			e.printStackTrace();
		}
	}

	@FXML
	/**
	 * Sets the focus to the pane when clicked
	 * 
	 * @param event
	 */
	void paneClicked(MouseEvent event) {
		pane.requestFocus();
		event.consume();
	}

	////////////////////////
	/// INSTANCE METHODS ///
	////////////////////////

	/**
	 * Attempts to locate a booking within the active and completed bookings tables
	 * of the park, specific to the park employee.
	 * 
	 * @param bookingID The booking ID to search for.
	 * @return The found Booking object, or null if no booking is found.
	 */
	private Booking findBookingAcrossTables(String bookingID) {
		String bookingId = bookingIDTxt.getText();
		String parkName = parkControl.nameOfTable(parkEmployee.getWorkingIn());
		String[] tables = { parkName + Communication.activeBookings, parkName + Communication.doneBookings,
				parkName + Communication.cancelledBookings };
		for (String table : tables) {
			try {
				Booking booking = parkControl.checkIfBookingExists(table, "bookingId", bookingId).get(0);
				return booking;
			} catch (NullPointerException e) {
				System.out.println("not exist in " + table);
			}
		}
		return null;
	}

	public ParkEmployee getParkEmployee() {
		return parkEmployee;
	}

	public void setParkEmployee(ParkEmployee parkEmployee) {
		this.parkEmployee = parkEmployee;
	}

	/**
	 * booking ID validation: Ensuring that the inserted ID is valid in terms of
	 * length and contains only digits.
	 * 
	 * @return return true if the bookingId is valid
	 */
	private boolean validate() {
		boolean valid = true;
		String error = "";
		String insertedID = bookingIDTxt.getText();
		if (insertedID.length() != 10 || !insertedID.matches("\\d+")) {
			error = "You must enter a valid booking ID number with exactly 10 digits";
			bookingIDTxt.setStyle(setFieldToError());
			valid = false;
			showErrorAlert(error);
		} 
		else
			bookingIDTxt.setStyle(setFieldToRegular());
		return valid;
	}

	private void updateParkCapacity() {
		String[] returnsVal = new String[4];
		returnsVal = parkControl.checkCurrentCapacity(parkEmployee.getWorkingIn().getParkName());
		if (returnsVal != null) {
			// sets the parameters
			parkEmployee.getWorkingIn().setMaximumVisitors(Integer.parseInt(returnsVal[0]));
			parkEmployee.getWorkingIn().setMaximumOrders(Integer.parseInt(returnsVal[1]));
			parkEmployee.getWorkingIn().setTimeLimit(Integer.parseInt(returnsVal[2]));
			parkEmployee.getWorkingIn().setCurrentCapacity(Integer.parseInt(returnsVal[3]));
		}
	}

	///////////////////////////////
	/// ABSTRACT SCREEN METHODS ///
	///////////////////////////////

	/**
	 * Initializes the screen with default settings, styles buttons, and loads the
	 * GoNature logo. Sets up the back button with an image and ensures labels and
	 * buttons are properly aligned.
	 */
	@Override
	public void initialize() {
		// Restore the park employee's user session.
		this.parkEmployee = (ParkEmployee) userControl.restoreUser();

		// Update the title label with the park name and emphasize it.
		titleLbl.setText(parkEmployee.getWorkingIn().getParkName() + " Park Entrance Management");
		titleLbl.underlineProperty();
		titleLbl.setAlignment(Pos.CENTER);
		titleLbl.layoutXProperty().bind(pane.widthProperty().subtract(titleLbl.widthProperty()).divide(2));

		// set booking labels
		fullNameLbl.setText("");
		dateLbl.setText("");
		sizeLbl.setText("");
		priceLbl.setText("");
		isPaidLbl.setText("");
		arrivalLbl.setText("");
		leavingLbl.setText("");
		statusLbl.setText("");

		// Set the GoNature logo.
		goNatureLogo.setImage(new Image(getClass().getResourceAsStream("/GoNatureBanner.png")));
		goNatureLogo.layoutXProperty().bind(pane.widthProperty().subtract(goNatureLogo.fitWidthProperty()).divide(2));

		// Set up the back button with a custom image, adjusting size and padding.
		ImageView backImage = new ImageView(new Image(getClass().getResourceAsStream("/backButtonImage.png")));
		backImage.setFitHeight(30);
		backImage.setFitWidth(30);
		backImage.setPreserveRatio(true);
		backButton.setGraphic(backImage);
		backButton.setPadding(new Insets(1, 1, 1, 1));

		entryTimeBtn.setDisable(true);
		exitTimeBtn.setDisable(true);
		invoiceBtn.setDisable(true);

		// setting the application's background
		setApplicationBackground(pane);
	}

	@Override
	public void loadBefore(Object information) {
		// irrelevant here
	}

	@Override
	/**
	 * Returns the screen's title
	 */
	public String getScreenTitle() {
		return parkEmployee.getWorkingIn().getParkName() + " - Park Entrance Management";
	}
}