package clientSide.gui;

import common.controllers.AbstractScreen;
import common.controllers.ScreenException;
import common.controllers.ScreenManager;
import common.controllers.StatefulException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class TotalNumberOfVisitorsReportController extends AbstractScreen {
	
	
	@FXML
    private Button backButton;

    @FXML
    private ImageView goNatureLogo;

    @FXML
    private Label TotalVisitorsReport;
    
    @FXML
    private PieChart TotalVisitorsPaiChart;
    
    @FXML
    void returnToPreviousScreen(ActionEvent event) {
    	try {
			ScreenManager.getInstance().goToPreviousScreen(true);
    	  } catch (ScreenException | StatefulException e) {
    	        e.printStackTrace();
    	  }
    }
	@Override
	public void initialize() {
		goNatureLogo.setImage(new Image(getClass().getResourceAsStream("/GoNatureBanner.png")));
		populateChart();
		 // setting the back button image
	 	ImageView backImage = new ImageView(new Image(getClass().getResourceAsStream("/backButtonImage.png")));
	 	backImage.setFitHeight(30);
	 	backImage.setFitWidth(30);
	 	backImage.setPreserveRatio(true);
	 	backButton.setGraphic(backImage);
	 	backButton.setPadding(new Insets(1, 1, 1, 1));
	}

	private void populateChart() {
		PieChart.Data singleVisitorsData = new PieChart.Data("Single visitors", 150);
        PieChart.Data groupVisitorsData = new PieChart.Data("Groups", 75);
      
        TotalVisitorsPaiChart.getData().addAll(singleVisitorsData, groupVisitorsData);
       
        TotalVisitorsPaiChart.getData().forEach(data ->data.nameProperty().bind(javafx.beans.binding.Bindings.concat(data.getName(), " ", data.pieValueProperty())));	
	}


	@Override
	public void loadBefore(Object information) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getScreenTitle() {
		// TODO Auto-generated method stub
		return null;
	}

}
