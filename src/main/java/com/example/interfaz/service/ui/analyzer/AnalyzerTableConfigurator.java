package com.example.interfaz.service.ui.analyzer;

import java.util.List;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxTreeTableCell;

public class AnalyzerTableConfigurator {

    public void configure(
            TreeTableView<AnalyzerRowModel> resultsTreeTable,
            TreeTableColumn<AnalyzerRowModel, Boolean> colSelect,
            TreeTableColumn<AnalyzerRowModel, String> colName,
            TreeTableColumn<AnalyzerRowModel, String> colArtist,
            TreeTableColumn<AnalyzerRowModel, String> colDuration,
            TreeTableColumn<AnalyzerRowModel, String> colSize,
            TreeTableColumn<AnalyzerRowModel, String> colType,
            TreeTableColumn<AnalyzerRowModel, String> colLanguage,
            TreeTableColumn<AnalyzerRowModel, String> colStatus,
            TreeTableColumn<AnalyzerRowModel, String> colPath,
            Runnable onSelectionChanged
    ) {
        TreeItem<AnalyzerRowModel> root = new TreeItem<>(new AnalyzerRowModel("Root", null, null));
        resultsTreeTable.setRoot(root);
        resultsTreeTable.setShowRoot(false);

        colSelect.setCellValueFactory(param -> {
            AnalyzerRowModel model = param.getValue().getValue();
            if (model == null || model.isGroup()) {
                return new SimpleBooleanProperty(false);
            }
            SimpleBooleanProperty prop = new SimpleBooleanProperty(model.getCandidate().isSelectedForDeletion());
            prop.addListener((obs, oldVal, newVal) -> {
                model.getCandidate().setSelectedForDeletion(newVal);
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
            });
            return prop;
        });
        colSelect.setCellFactory(CheckBoxTreeTableCell.forTreeTableColumn(colSelect));
        colSelect.setEditable(true);
        resultsTreeTable.setEditable(true);

        colName.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getName)));
        colArtist.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getArtist)));
        colDuration.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getDurationFormatted)));
        colSize.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getSizeFormatted)));
        colType.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getType)));
        colLanguage.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getLanguageFormatted)));
        colStatus.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getStatus)));
        colPath.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getPath)));
    }

    public void populate(TreeTableView<AnalyzerRowModel> resultsTreeTable, List<DuplicateGroup> groups) {
        if (resultsTreeTable == null) {
            return;
        }
        TreeItem<AnalyzerRowModel> root = resultsTreeTable.getRoot();
        if (root == null) {
            root = new TreeItem<>(new AnalyzerRowModel("Root", null, null));
            resultsTreeTable.setRoot(root);
        }
        root.getChildren().clear();

        if (groups == null) {
            return;
        }

        for (DuplicateGroup group : groups) {
            AnalyzerRowModel groupModel = new AnalyzerRowModel(
                    group.getGroupName() + " (" + group.getCandidates().size() + " archivos)",
                    group,
                    null
            );
            TreeItem<AnalyzerRowModel> groupItem = new TreeItem<>(groupModel);
            groupItem.setExpanded(true);

            for (DuplicateCandidate candidate : group.getCandidates()) {
                AnalyzerRowModel candidateModel = new AnalyzerRowModel(
                        candidate.getSongFile().getFileName(),
                        group,
                        candidate
                );
                TreeItem<AnalyzerRowModel> candidateItem = new TreeItem<>(candidateModel);
                groupItem.getChildren().add(candidateItem);
            }

            root.getChildren().add(groupItem);
        }
    }

    private String getValueSafely(TreeTableColumn.CellDataFeatures<AnalyzerRowModel, String> param, java.util.function.Function<AnalyzerRowModel, String> extractor) {
        if (param != null && param.getValue() != null && param.getValue().getValue() != null) {
            return extractor.apply(param.getValue().getValue());
        }
        return "";
    }
}
