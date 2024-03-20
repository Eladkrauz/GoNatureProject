package clientSide.gui;

import clientSide.control.ParkController;
import common.controllers.AbstractScreen;
import entities.Booking;
import entities.ParkVisitor;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

/**
 * The CancellationScreenController is called after a booking if successfully
 * cancelled, showing a cancellation confirmation to the user.
 */
public class CancellationScreenController extends AbstractScreen {
	private ParkVisitor visitor;

	//////////////////////////////////
	/// JAVAFX ANF FXML COMPONENTS ///
	//////////////////////////////////
	
	@FXML
	private Label bookingIdLabel, dateLabel, emailLabel, holderLabel, isPaidLabel, parkAddressLabel, parkNameLabel,
			phoneLabel, priceLabel, timeLabel, visitorsLabel;
	@FXML
	private ImageView goNatureLogo, parkImage;
	@FXML
	private Button returnToAccountBtn;
	@FXML
	private Pane pane;

	/////////////////////
	/// EVENT METHODS ///
	/////////////////////

	@FXML
	/**
	 * This method is called after the user clicked on "Return to Account" button
	 * 
	 * @param event
	 */
	void returnToAccount(ActionEvent event) {
		/// HERE: only connected visitors arrive!
		if (visitor == null) { // is not connected to the system, entered only with id
			showInformationAlert("Now returning to main screen.");
		} else {
			showInformationAlert("Now returning to account screen.");
		}
	}

	///////////////////////////////
	/// ABSTRACT SCREEN METHODS ///
	///////////////////////////////

	@Override
	/**
	 * This method initialized all the fxml and javafx components
	 */
	public void initialize() {
		// initializing the image component and centering it
		goNatureLogo.setImage(new Image(getClass().getResourceAsStream("/GoNatureBanner.png")));
		goNatureLogo.layoutXProperty().bind(pane.widthProperty().subtract(goNatureLogo.fitWidthProperty()).divide(2));
	}

	@Override
	/**
	 * This method gets a pair object of booking and park visitor and sets the GUI
	 * components with information from these instances.
	 */
	public void loadBefore(Object information) {
		Booking booking;
		if (information != null && information instanceof Pair) {
			@SuppressWarnings("unchecked")
			Pair<Booking, ParkVisitor> pair = (Pair<Booking, ParkVisitor>) information;
			booking = pair.getKey();
			visitor = pair.getValue();

			parkNameLabel.setText("Park Name: " + booking.getParkBooked().getParkName());
			parkAddressLabel.setText("Park Location: " + booking.getParkBooked().getParkCity() + ", "
					+ booking.getParkBooked().getParkState());
			bookingIdLabel.setText("Booking ID: " + booking.getBookingId());
			holderLabel.setText("Full Name: " + booking.getFirstName() + " " + booking.getLastName());
			emailLabel.setText("Email: " + booking.getEmailAddress());
			phoneLabel.setText("Phone: " + booking.getPhoneNumber());
			dateLabel.setText("Date of Visit: " + booking.getDayOfVisit() + "");
			timeLabel.setText("Time of Visit: " + booking.getTimeOfVisit() + "");
			visitorsLabel.setText("Group Size: " + booking.getNumberOfVisitors() + "");
			priceLabel.setText("Final Price: " + booking.getFinalPrice() + "$");
			if (booking.isPaid()) {
				isPaidLabel.setText("Your reservation was paid. We will initiate a refund in the next 48 hours.");
			} else {
				isPaidLabel.setText("Your reservation was not paid.");
			}

			String parkImagePath = "/" + ParkController.getInstance().nameOfTable(booking.getParkBooked()) + ".jpg";
			parkImage.setImage(new Image(getClass().getResourceAsStream(parkImagePath)));

		}
	}

	@Override
	/**
	 * Returns the screen's title
	 */
	public String getScreenTitle() {
		return "Reservation Confirmation";
	}
}