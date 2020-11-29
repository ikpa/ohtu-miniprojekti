
package gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import java.sql.SQLException;
import javafx.util.converter.IntegerStringConverter;

import logic.Book;
import logic.BookmarkService;

import dao.Database;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import dao.BookmarkDao;
import java.util.ArrayList;
import java.util.Iterator;
import javafx.event.EventType;

public class GUI extends Application {
    
    private final int windowWidth = 500;
    private final int windowHeight = 500;
    
    private Scene mainMenu;
    private Scene addRecommendation;
    private BorderPane layout;
    
    private Stage stage;
    private BookmarkDao service;
    
    private ObservableList<Book> bookList;
    private ArrayList<Book> fullBookList;
    private ListView<Book> listView;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws SQLException {
        Database helper;
        String isTest = System.getProperty("isTestEnvironment");
        if (isTest != null && isTest.equals("true")) {
            helper = new Database(":memory:");
        } else {
            helper = new Database("lukuvinkki.db");
        }
        try {
            service = new BookmarkService(helper);
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    @Override
    public void start(Stage stage) throws SQLException {
        
        this.stage = stage;
        this.stage.setTitle("Lukuvinkkikirjanpito");
        mainMenu = mainMenu();
        addRecommendation = addBookmark();
        
        this.stage.setScene(mainMenu);
        this.stage.show();
    }
    
    private Scene mainMenu() throws SQLException {
        
        layout = new BorderPane();

        Button addBookmark = new Button();
        addBookmark.setText("Lisää kirja");
        addBookmark.setId("add");
        
        
        HBox menu = new HBox(10);
        menu.getChildren().addAll(addBookmark);
        menu.setAlignment(Pos.CENTER);
        
        layout.setTop(menu);
        listBookmarks();
        layout.setCenter(listView);
        
        TextField searchField = new TextField();
        searchField.setId("search");
        Label searchByTitle = new Label("Etsi nimellä");
        menu.getChildren().addAll(searchByTitle, searchField);
        menu.setSpacing(10);
        
        searchField.textProperty().addListener(obs -> {
            String filter = searchField.getText();
            bookList.clear();
            bookList.addAll(fullBookList);
            if (filter != null && filter.length() > 0) {
                for (Iterator<Book> iterator = bookList.iterator(); iterator.hasNext();) {
                    Book item = iterator.next();
                    if (!item.getTitle().toLowerCase().contains(filter.toLowerCase())) {
                        iterator.remove();
                    }
                }
            }
            listView.setItems(bookList);
            listView.refresh();
            
        });
        
        addBookmark.setOnAction(e -> stage.setScene(addRecommendation));
        
        
        return new Scene(layout, windowWidth, windowHeight);
    }

    private void listBookmarks() throws SQLException {
        fullBookList = new ArrayList<>(service.getAllBooks());
        bookList = FXCollections.observableArrayList(fullBookList);
        listView = new ListView<>(bookList);
        listView.setId("listview");
        listView.setCellFactory(param -> new CustomCell(service, 
                listView, bookList, fullBookList));
        
    }
    
    private Scene addBookmark() {
        VBox addLayout = new VBox(10);
        addLayout.setPadding(new Insets(10, 10, 10, 10));
        addLayout.setId("addview");
        
        Label createNewRecommendation = new Label("Luo uusi lukuvinkki");
        Button add = new Button("Lisää!");
        Label error = new Label("");
        error.setId("errorMessage");
        add.setId("submit");
        Label titleLabel = new Label("Otsikko: ");
        TextField titleInput = new TextField();
        titleInput.setId("name");
        titleInput.setMaxWidth(350);
        Label authorLabel = new Label("Kirjailija: ");
        TextField authorInput = new TextField();
        authorInput.setId("author");
        authorInput.setMaxWidth(350);
        Label pageCountLabel = new Label("Sivumaara: ");
        TextField pageCountInput = new TextField();
        pageCountInput.setId("pageCount");
        pageCountInput.setTextFormatter((new TextFormatter<>(new IntegerStringConverter())));
        pageCountInput.setMaxWidth(100);
        Button back = new Button("Takaisin");
        back.setId("back");
        
        addLayout.getChildren().addAll(back, createNewRecommendation, titleLabel, titleInput, authorLabel,
            authorInput, pageCountLabel, pageCountInput, error, add);
        
        add.setOnAction(e -> {
            String title = titleInput.getText().trim();
            String author = authorInput.getText().trim();
            String pageCount = pageCountInput.getText().trim();
            if (title.isEmpty() || author.isEmpty() || pageCount.isEmpty()) {
                error.setText("Täytä kaikki tiedot.");
                return;
            }
            int pageCountInt = Integer.parseInt(pageCount);
            try {
                Book book = new Book(title, author, pageCountInt);
                if (!service.addBook(book)) {
                    error.setText("Samanniminen kirja on jo lisätty sovellukseen.");
                    return;
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
            returnToMainPage();
        });
        
        back.setOnAction(e -> {
            returnToMainPage();
        });
        
        return new Scene(addLayout, windowWidth, windowHeight);
    }
    
    private void returnToMainPage() {
        try {
            listBookmarks();
        } catch (SQLException e) {
            System.out.println("returnToMainPage error: " + e.getMessage());
        }
        layout.setCenter(listView);

        stage.setScene(mainMenu);
    }

}