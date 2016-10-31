/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multisocketserverfx;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import socketfx.FxMultipleSocketServer;
import socketfx.SocketListener;


/**
 *
 * @author jtconnor
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private Button clearRcvdMsgsButton;
    @FXML
    private Button clearSentMsgsButton;
    @FXML
    private TextField sendTextField;
    @FXML
    private TextField selectedTextField;
    @FXML
    private Button sendButton;
    @FXML
    private Label connectionsLabel;
    @FXML
    private CheckBox autoConnectCheckBox;
    @FXML
    private Button connectButton;
    @FXML
    private Button disconnectButton;
    @FXML
    private TextField portTextField;
    @FXML
    private ListView<String> rcvdMsgsListView;
    private ObservableList<String> sentMsgsData;
    @FXML
    private ListView<String> sentMsgsListView;
    private ObservableList<String> rcvdMsgsData;

    private FxMultipleSocketServer socketServer;
    private ListView lastSelectedListView;

    class FxSocketListener implements SocketListener {

        @Override
        public void onMessage(String line) {
            if (line != null && !line.equals("")) {
                rcvdMsgsData.add(line);
            }
        }

        @Override
        public void onClosedStatus(boolean isClosed) {
            int numConnections = socketServer.getListenerCount();
            if (numConnections == 0) {
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                sendButton.setDisable(true);
                sendTextField.setDisable(true);
            } else {
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                sendButton.setDisable(false);
                sendTextField.setDisable(false);
            }
            StringBuilder connectionsSB
                    = new StringBuilder(numConnections + " connection");
            if (numConnections != 1) {
                connectionsSB.append("s");
            }
            connectionsLabel.setText(new String(connectionsSB));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connectButton.setDisable(false);
        sendButton.setDisable(true);
        sendTextField.setDisable(true);
        disconnectButton.setDisable(true);
        sentMsgsData = FXCollections.observableArrayList();
        sentMsgsListView.setItems(sentMsgsData);
        sentMsgsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        sentMsgsListView.setOnMouseClicked(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                String selectedItem = 
                        sentMsgsListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.equals("null")) {
                    selectedTextField.setText("Sent: " + selectedItem);
                    lastSelectedListView = sentMsgsListView;
                }
            }
        });

        rcvdMsgsData = FXCollections.observableArrayList();
        rcvdMsgsListView.setItems(rcvdMsgsData);
        rcvdMsgsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        rcvdMsgsListView.setOnMouseClicked(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                String selectedItem = 
                        rcvdMsgsListView.getSelectionModel().getSelectedItem();
                if (selectedItem != null && !selectedItem.equals("null")) {
                    selectedTextField.setText("Received: " + selectedItem);
                    lastSelectedListView = rcvdMsgsListView;
                }
            }
        });

    }

    @FXML
    private void handleClearRcvdMsgsButton(ActionEvent event) {
        rcvdMsgsData.clear();
        if (lastSelectedListView == rcvdMsgsListView) {
            selectedTextField.clear();
        }
    }

    @FXML
    private void handleClearSentMsgsButton(ActionEvent event) {
        sentMsgsData.clear();
        if (lastSelectedListView == sentMsgsListView) {
            selectedTextField.clear();
        }
    }

    @FXML
    private void handleSendMessageButton(ActionEvent event) {
        if (!sendTextField.getText().equals("")) {
            socketServer.postUpdate(sendTextField.getText());
            sentMsgsData.add(sendTextField.getText());
        }
    }

    @FXML
    private void handleConnectButton(ActionEvent event) {
        socketServer = new FxMultipleSocketServer(new FxSocketListener(),
                Integer.valueOf(portTextField.getText()));
        new Thread(socketServer).start();
    }

    @FXML
    private void handleDisconnectButton(ActionEvent event) {
        socketServer.shutdown();
    }

}
