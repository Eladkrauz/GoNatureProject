package clientSide.gui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import clientSide.control.BookingController;
import clientSide.control.ParkController;
import common.communication.Communication;
import common.controllers.AbstractScreen;
import common.controllers.ScreenException;
import common.controllers.ScreenManager;
import common.controllers.StatefulException;
import entities.Booking;
import entities.Booking.VisitType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

public class RescheduleScreenController extends AbstractScreen {
	private BookingController control;
	private Booking booking;
	private boolean isGroupReservation;

	public RescheduleScreenController() {
		control = BookingController.getInstance();
	}

	////////////////////////////////////
	/// INNER CLASS - AVAILABLE SLOT ///
	////////////////////////////////////

	/**
	 * A class for holding an available slot
	 */
	public static class AvailableSlot {
		private LocalDate date;
		private LocalTime time;

		/**
		 * Constructor for the avaialble slot
		 */
		public AvailableSlot(LocalDate date, LocalTime time) {
			this.date = date;
			this.time = time;
		}

		/**
		 * @return the date
		 */
		public LocalDate getDate() {
			return date;
		}

		/**
		 * @return the time
		 */
		public LocalTime getTime() {
			return time;
		}

		/**
		 * @param date
		 */
		public void setDate(LocalDate date) {
			this.date = date;
		}

		/**
		 * @param time
		 */
		public void setTime(LocalTime time) {
			this.time = time;
		}

		@Override
		public String toString() {
			return "AvailableSlot [date=" + date + ", time=" + time + "]";
		}
	}

	//////////////////////////////////
	/// FXML AND JAVAFX COMPONENTS ///
	//////////////////////////////////

	@FXML
	private TableView<AvailableSlot> availableTable;
	@FXML
	private TableColumn<AvailableSlot, LocalDate> dateColumn;
	@FXML
	private TableColumn<AvailableSlot, LocalTime> arrivalColumn;

	@FXML
	private Button backButton, showBtn;

	@FXML
	private ImageView goNatureLogo;

	@FXML
	private Pane pane;

	@FXML
	private Label titleLbl, typeLbl, bookingLbl, waitLabel, instructionsLbl;

	@FXML
	private DatePicker fromDate, toDate;

	@FXML
	private ProgressIndicator progressIndicator;

	//////////////////////////////
	/// EVENT HANDLING METHODS ///
	//////////////////////////////

	@FXML
	void showClicked(ActionEvent event) {
		if (!validate()) {
			event.consume();
			return;
		} else {
			// showing the loading dialog
			setVisible(false);

			// moving the data fetching operation to a background thread
			new Thread(() -> {
				// this operation is now off the JavaFX Application Thread
				final ObservableList<AvailableSlot> newSlots = FXCollections.observableArrayList(
						control.getParkAvailabilitySlots(booking, fromDate.getValue(), toDate.getValue()));

				// once fetching is complete, updating the UI on the JavaFX Application Thread
				Platform.runLater(() -> {
					availableTable.setItems(newSlots);
					// closing the loading dialog after updating the table
					setVisible(true);
				});
			}).start();
			availableTable.setDisable(false);
		}
	}

	@FXML
	/**
	 * In case the pane clicked, taking the focus
	 * 
	 * @param event
	 */
	void paneClicked(MouseEvent event) {
		pane.requestFocus();
	}

	@FXML
	void paneTabPressed(KeyEvent event) {
		event.consume();
	}

	@FXML
	/**
	 * Returns to the previous screen
	 * 
	 * @param event
	 */
	void returnToPreviousScreen(ActionEvent event) {
		try {
			ScreenManager.getInstance().goToPreviousScreen(true, true);
		} catch (ScreenException | StatefulException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is called after the user double clicked on a slot from the table
	 * 
	 * @param event
	 */
	public void slotClicked(AvailableSlot chosenSlot) {
		if (chosenSlot == null) {
			showErrorAlert("Please choose a row from the table in order to proceed");
		} else {
			LocalDate newDate = chosenSlot.getDate();
			LocalTime newTime = chosenSlot.getTime();
			int choise = showConfirmationAlert(
					"You're about to change your reservation dates:\n" + "From: " + booking.getDayOfVisit() + ", "
							+ booking.getTimeOfVisit() + ", To: " + newDate + ", " + newTime,
					Arrays.asList("Cancel", "Confirm"));

			switch (choise) { // chose to cancel the operation
			case 1: {
				return;
			}

			case 2: { // confirmed the editing
				booking.setDayOfVisit(chosenSlot.getDate());
				booking.setTimeOfVisit(chosenSlot.getTime());
			}

				// first inserting the new booking to the database to update capacities and save
				// the visitor's spot
				control.insertNewBookingToActiveTable(booking);

				int discountPrice = control.calculateFinalDiscountPrice(booking, isGroupReservation, false);
				int preOrderPrice = control.calculateFinalDiscountPrice(booking, true, true);

				// creating the pop up message
				String payMessage = "Woohoo! You're almost set.";
				if (isGroupReservation) {
					payMessage += "\nPay now and get a special discount for paying ahead:";
					payMessage += "\n        Your reservation's final price: " + discountPrice + "$";
					payMessage += "\n        Your reservetion's price after paying ahead discount: " + preOrderPrice
							+ "$";
				} else {
					payMessage += "\nYour reservation's final price (after pre-order discount) is: " + discountPrice
							+ "$";
				}

				int payChoise = showConfirmationAlert(payMessage,
						Arrays.asList("Pay Now", "Pay Upon Arrival", "Cancel Reservation"));

				switch (payChoise) {
				// chose to pay now
				case 1: {
					booking.setPaid(true);
					booking.setFinalPrice(isGroupReservation ? preOrderPrice : discountPrice);
					// updating the payment columns in the database
					control.updateBookingPayment(booking);
					// showing the payment screen
					try {
						ScreenManager.getInstance().showScreen("PaymentSystemScreenController",
								"/clientSide/fxml/PaymentSystemScreen.fxml", true, false,
								new Pair<Booking, String>(booking, "online"));
					} catch (StatefulException | ScreenException e) {
						e.printStackTrace();
					}
					return;
				}

				// chose to pay upon arrival
				case 2: {
					booking.setPaid(false);
					booking.setFinalPrice(discountPrice);
					// updating the payment columns in the database
					control.updateBookingPayment(booking);
					// showing the confirmation screen
					try {
						ScreenManager.getInstance().showScreen("ConfirmationScreenController",
								"/clientSide/fxml/ConfirmationScreen.fxml", true, false, booking);
					} catch (StatefulException | ScreenException e) {
						e.printStackTrace();
					}
					return;
				}

				// chose to cancel reservation
				case 3: {
					// deleting the new booking from the database cause it wat inserted in order to
					// save the spot for the visitor, and returning to acount screen
					control.deleteBooking(booking, Communication.activeBookings);
					returnToPreviousScreen(null);
					return;
				}
				}
			}
		}
	}

	////////////////////////
	/// INSTANCE METHODS ///
	////////////////////////

	/**
	 * This method validates the entered dates for the date range
	 * 
	 * @return true if valid, false if not
	 */
	private boolean validate() {
		// set styles to regular
		fromDate.setStyle(setFieldToRegular());
		toDate.setStyle(setFieldToRegular());

		// validating the entered dates
		String error = "Please make sure you:\n";
		boolean valid = true;

		// checking the 'from' date
		if (fromDate.getValue() == null) {
			fromDate.setStyle(setFieldToError());
			error += "• enter a 'from' date";
			valid = false;
		} else {
			if (fromDate.getValue().compareTo(LocalDate.now()) < 0) { // past
				fromDate.setStyle(setFieldToError());
				error += "• choose a 'from' date that is " + LocalDate.now() + " and on\n";
				valid = false;
			} else { // future
				// calculating the date that is in the future allowed range
				LocalDate maximumFutureRange = (LocalDate.now()).plusMonths(control.futureBookingsRange);

				if (fromDate.getValue().compareTo(maximumFutureRange) > 0) {
					fromDate.setStyle(setFieldToError());
					error += "• choose a 'from' date that is before " + maximumFutureRange + "\n";
					valid = false;
				}
			}
		}

		// checking the 'to' date
		if (toDate.getValue() == null) {
			toDate.setStyle(setFieldToError());
			error += "• enter a 'to' date";
			valid = false;
		} else {
			if (toDate.getValue().compareTo(LocalDate.now()) < 0) { // past
				toDate.setStyle(setFieldToError());
				error += "• choose a 'to' date that is " + LocalDate.now() + " and on\n";
				valid = false;
			} else { // future
				// calculating the date that is in the future allowed range
				LocalDate maximumFutureRange = (LocalDate.now()).plusMonths(control.futureBookingsRange);

				if (toDate.getValue().compareTo(maximumFutureRange) > 0) {
					toDate.setStyle(setFieldToError());
					error += "• choose a 'to' date that is before " + maximumFutureRange + "\n";
					valid = false;
				}
			}
		}

		// checking that 'from' is smaller than 'to'
		if (fromDate.getValue() != null && toDate.getValue() != null) {
			if (toDate.getValue().compareTo(fromDate.getValue()) < 0) {
				fromDate.setStyle(setFieldToError());
				toDate.setStyle(setFieldToError());
				error += "• the 'to' date must be later than the 'from' date\n";
				valid = false;
			}
		}

		if (!valid)
			showErrorAlert(error);

		return valid;
	}

	/**
	 * This method is used to hide/show all elements but the progress indicator and
	 * its label
	 * 
	 * @param visible
	 */
	private void setVisible(boolean visible) {
		progressIndicator.setVisible(!visible);
		waitLabel.setVisible(!visible);
		availableTable.setVisible(visible);
		instructionsLbl.setVisible(visible);
	}

	///////////////////////////////
	/// ABSTRACT SCREEN METHODS ///
	///////////////////////////////

	@Override
	/**
	 * Initializes the FXML components
	 */
	public void initialize() {
		// initializing the image component and centering it
		goNatureLogo.setImage(new Image(getClass().getResourceAsStream("/GoNatureBanner.png")));
		goNatureLogo.layoutXProperty().bind(pane.widthProperty().subtract(goNatureLogo.fitWidthProperty()).divide(2));

		dateColumn.setResizable(false);
		arrivalColumn.setResizable(false);

		titleLbl.setAlignment(Pos.CENTER);
		titleLbl.layoutXProperty().bind(pane.widthProperty().subtract(titleLbl.widthProperty()).divide(2));
		bookingLbl.getStyleClass().add("label-center-right");

		// setting the back button image
		ImageView backImage = new ImageView(new Image(getClass().getResourceAsStream("/backButtonImage.png")));
		backImage.setFitHeight(30);
		backImage.setFitWidth(30);
		backImage.setPreserveRatio(true);
		backButton.setGraphic(backImage);
		backButton.setPadding(new Insets(1, 1, 1, 1));

		// setting the table's columns
		dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
		arrivalColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

		// setting the empty-table labels
		availableTable.setPlaceholder(new Label("Select dates range in order to proceed"));

		// setting what will occur when double-clicking on a row of the future table
		availableTable.setRowFactory(tv -> {
			TableRow<AvailableSlot> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
					AvailableSlot clickedRowData = row.getItem();
					slotClicked(clickedRowData);
				}
			});
			return row;
		});

		availableTable.setVisible(false);
		instructionsLbl.setVisible(false);
		progressIndicator.setVisible(false);
		waitLabel.setVisible(false);

		// setting the labels
		waitLabel.setAlignment(Pos.CENTER);
		waitLabel.layoutXProperty().bind(pane.widthProperty().subtract(waitLabel.widthProperty()).divide(2));
		waitLabel.setText("We are looking for available slots for your group\nThis could take several seconds...");
		waitLabel.setStyle("-fx-text-alignment: center;");
		instructionsLbl.setAlignment(Pos.CENTER);
		instructionsLbl.layoutXProperty().bind(pane.widthProperty().subtract(waitLabel.widthProperty()).divide(2));
		instructionsLbl.setStyle("-fx-text-alignment: center;");
		instructionsLbl.getStyleClass().add("label-stroke");

		// setting the porgress indicator
		progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		progressIndicator.layoutXProperty()
				.bind(pane.widthProperty().subtract(progressIndicator.widthProperty()).divide(2));

		// setting the application's background
		setApplicationBackground(pane);
	}

	@Override
	/**
	 * Before showing the screen, a bookings instance is transfered to this
	 * controller in order to load its information into the GUI components
	 */
	public void loadBefore(Object information) {
		if (information instanceof Booking) {
			booking = (Booking) information;

			isGroupReservation = booking.getVisitType() == VisitType.GROUP ? true : false;

			// setting the reservation type
			typeLbl.setText("Review available slots in " + booking.getParkBooked().getParkName() + " park");

			// setting the booking id
			bookingLbl.setText("Booking ID: " + booking.getBookingId());

			// setting the background image
			ImageView backgroundImage = new ImageView(
					new Image("/" + ParkController.getInstance().nameOfTable(booking.getParkBooked()) + ".jpg"));

			backgroundImage.fitWidthProperty().bind(ScreenManager.getInstance().getStage().widthProperty());
			backgroundImage.fitHeightProperty().bind(ScreenManager.getInstance().getStage().heightProperty());
			backgroundImage.setPreserveRatio(false);
			backgroundImage.setOpacity(0.2);

			if (pane.getChildren().get(0) instanceof ImageView) {
				pane.getChildren().remove(0);
			}
			pane.getChildren().add(0, backgroundImage);
		}
	}

	@Override
	/**
	 * Returns the screen's title
	 */
	public String getScreenTitle() {
		return "Reschedule";
	}
}