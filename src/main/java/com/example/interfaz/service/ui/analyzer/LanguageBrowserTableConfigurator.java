package com.example.interfaz.service.ui.analyzer;

import com.example.interfaz.viewmodel.LibraryAnalyzerViewModel;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;

public class LanguageBrowserTableConfigurator {

    public void configure(
            TableView<LanguageSongRowModel> langBrowserTable,
            TableColumn<LanguageSongRowModel, Boolean> colLangSelect,
            TableColumn<LanguageSongRowModel, String> colLangName,
            TableColumn<LanguageSongRowModel, String> colLangArtist,
            TableColumn<LanguageSongRowModel, String> colLangLang,
            TableColumn<LanguageSongRowModel, String> colLangConf,
            TableColumn<LanguageSongRowModel, String> colLangMethod,
            TableColumn<LanguageSongRowModel, String> colLangFormat,
            TableColumn<LanguageSongRowModel, String> colLangSize,
            TableColumn<LanguageSongRowModel, String> colLangPath,
            ComboBox<String> cmbLangFilter,
            TextField txtLangSearch,
            Label lblLangCount,
            LibraryAnalyzerViewModel viewModel
    ) {
        if (cmbLangFilter != null) {
            cmbLangFilter.getItems().setAll(
                    "Todos", "Español", "Inglés", "Portugués", "Francés",
                    "Japonés", "Coreano", "Chino", "Ambiguo",
                    "Mixto / Bilingüe", "Desconocido"
            );
            cmbLangFilter.getSelectionModel().select("Todos");

            cmbLangFilter.valueProperty().addListener((obs, old, nv) -> {
                viewModel.langFilterProperty().set(nv != null ? nv : "Todos");
                refreshLangCount(lblLangCount, viewModel);
            });
        }

        if (txtLangSearch != null) {
            txtLangSearch.textProperty().bindBidirectional(viewModel.langSearchProperty());
            txtLangSearch.textProperty().addListener((obs, old, nv) -> refreshLangCount(lblLangCount, viewModel));
        }

        if (langBrowserTable != null) {
            langBrowserTable.setEditable(true);

            if (colLangSelect != null) {
                colLangSelect.setCellValueFactory(param -> {
                    LanguageSongRowModel model = param.getValue();
                    SimpleBooleanProperty prop = new SimpleBooleanProperty(model.isSelected());
                    prop.addListener((obs, old, nv) -> {
                        model.setSelected(nv);
                        viewModel.updateLangBrowserSummary();
                    });
                    return prop;
                });
                colLangSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colLangSelect));
                colLangSelect.setEditable(true);
            }

            if (colLangName != null) {
                colLangName.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getName() : ""));
            }

            if (colLangArtist != null) {
                colLangArtist.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getArtist() : ""));
            }

            if (colLangLang != null) {
                colLangLang.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getLanguage() : ""));
                colLangLang.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            setStyle(getLangStyle(item));
                        }
                    }
                });
            }

            if (colLangConf != null) {
                colLangConf.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getConfidence() : ""));
            }

            if (colLangMethod != null) {
                colLangMethod.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getMethod() : ""));
            }

            if (colLangFormat != null) {
                colLangFormat.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getFormat() : ""));
            }

            if (colLangSize != null) {
                colLangSize.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getSizeFormatted() : ""));
            }

            if (colLangPath != null) {
                colLangPath.setCellValueFactory(param -> new SimpleStringProperty(
                        param.getValue() != null ? param.getValue().getPath() : ""));
            }

            langBrowserTable.setItems(viewModel.getFilteredSongModels());
        }

        viewModel.getFilteredSongModels().addListener((ListChangeListener<LanguageSongRowModel>) c ->
                refreshLangCount(lblLangCount, viewModel));
    }

    public void refreshLangCount(Label lblLangCount, LibraryAnalyzerViewModel viewModel) {
        if (lblLangCount != null && viewModel != null) {
            int count = viewModel.getFilteredSongModels().size();
            lblLangCount.setText(count + " canción" + (count == 1 ? "" : "es"));
        }
    }

    private String getLangStyle(String langName) {
        if (langName == null) return "";
        if (langName.startsWith("Ambiguo")) {
            return "-fx-text-fill: #FFC107; -fx-font-weight: bold;";
        }
        return switch (langName) {
            case "Español" -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
            case "Inglés" -> "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
            case "Portugués" -> "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
            case "Francés" -> "-fx-text-fill: #9C27B0; -fx-font-weight: bold;";
            case "Japonés", "Coreano", "Chino" -> "-fx-text-fill: #F44336; -fx-font-weight: bold;";
            case "Mixto / Bilingüe" -> "-fx-text-fill: #795548; -fx-font-weight: bold;";
            default -> "-fx-text-fill: -color-fg-muted;";
        };
    }
}
