package com.example.interfaz.service.ui.analyzer;

import java.util.List;

import com.example.interfaz.model.analyzer.DuplicateCandidate;
import com.example.interfaz.model.analyzer.DuplicateGroup;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;

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
        resultsTreeTable.setEditable(true);

        resultsTreeTable.setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(AnalyzerRowModel item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("analyzer-group-row", "analyzer-child-row", "analyzer-original-row", "analyzer-deletion-row");
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isGroup()) {
                    getStyleClass().add("analyzer-group-row");
                } else {
                    getStyleClass().add("analyzer-child-row");
                    if (item.getCandidate() != null) {
                        if (item.getCandidate().isOriginal()) {
                            getStyleClass().add("analyzer-original-row");
                        } else if (item.getCandidate().isSelectedForDeletion()) {
                            getStyleClass().add("analyzer-deletion-row");
                        }
                    }
                }
            }
        });

        colSelect.setCellValueFactory(param -> {
            AnalyzerRowModel model = getValueModel(param);
            if (model == null) {
                return new SimpleBooleanProperty(false);
            }
            if (model.isGroup()) {
                boolean allCopiesSelected = model.getGroup() != null && !model.getGroup().getCandidates().isEmpty()
                        && model.getGroup().getCandidates().stream().filter(c -> !c.isOriginal()).allMatch(DuplicateCandidate::isSelectedForDeletion);
                return new SimpleBooleanProperty(allCopiesSelected);
            }
            if (model.getCandidate() != null) {
                if (model.getCandidate().isOriginal()) {
                    return new SimpleBooleanProperty(false);
                }
                return new SimpleBooleanProperty(model.getCandidate().isSelectedForDeletion());
            }
            return new SimpleBooleanProperty(false);
        });

        colSelect.setCellFactory(column -> new TreeTableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    TreeItem<AnalyzerRowModel> treeItem = getTableRow() != null ? getTableRow().getTreeItem() : null;
                    if (treeItem == null || treeItem.getValue() == null) return;
                    AnalyzerRowModel model = treeItem.getValue();
                    boolean isChecked = checkBox.isSelected();
                    if (model.isGroup()) {
                        if (model.getGroup() != null) {
                            for (DuplicateCandidate c : model.getGroup().getCandidates()) {
                                if (!c.isOriginal()) {
                                    c.setSelectedForDeletion(isChecked);
                                }
                            }
                        }
                    } else if (model.getCandidate() != null && !model.getCandidate().isOriginal()) {
                        model.getCandidate().setSelectedForDeletion(isChecked);
                    }
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
                    resultsTreeTable.refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getTreeItem() == null) {
                    setGraphic(null);
                } else {
                    AnalyzerRowModel model = getTableRow().getTreeItem().getValue();
                    if (model == null) {
                        setGraphic(null);
                    } else if (model.isGroup()) {
                        boolean allCopiesSelected = model.getGroup() != null && !model.getGroup().getCandidates().isEmpty()
                                && model.getGroup().getCandidates().stream().filter(c -> !c.isOriginal()).allMatch(DuplicateCandidate::isSelectedForDeletion);
                        checkBox.setSelected(allCopiesSelected);
                        checkBox.setDisable(false);
                        setGraphic(checkBox);
                    } else if (model.getCandidate() != null) {
                        if (model.getCandidate().isOriginal()) {
                            checkBox.setSelected(false);
                            checkBox.setDisable(true);
                            setGraphic(checkBox);
                        } else {
                            checkBox.setSelected(model.getCandidate().isSelectedForDeletion());
                            checkBox.setDisable(false);
                            setGraphic(checkBox);
                        }
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        colName.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getDisplayName)));
        colArtist.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getArtist)));
        colDuration.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getDurationFormatted)));
        colSize.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getSizeFormatted)));
        colType.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getType)));
        colLanguage.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getLanguageFormatted)));

        colStatus.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getStatusClean)));
        colStatus.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    TreeItem<AnalyzerRowModel> treeItem = getTableRow() != null ? getTableRow().getTreeItem() : null;
                    if (treeItem != null && treeItem.getValue() != null) {
                        String details = treeItem.getValue().getScoreDetails();
                        if (details != null && !details.isEmpty()) {
                            setTooltip(new Tooltip(details));
                        } else {
                            setTooltip(null);
                        }
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        colPath.setCellValueFactory(param -> new SimpleStringProperty(getValueSafely(param, AnalyzerRowModel::getAbbreviatedPath)));
        colPath.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    TreeItem<AnalyzerRowModel> treeItem = getTableRow() != null ? getTableRow().getTreeItem() : null;
                    if (treeItem != null && treeItem.getValue() != null) {
                        String fullPath = treeItem.getValue().getPath();
                        if (fullPath != null && !fullPath.isEmpty()) {
                            setTooltip(new Tooltip(fullPath));
                        } else {
                            setTooltip(null);
                        }
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
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

    private AnalyzerRowModel getValueModel(TreeTableColumn.CellDataFeatures<AnalyzerRowModel, ?> param) {
        if (param != null && param.getValue() != null) {
            return param.getValue().getValue();
        }
        return null;
    }

    private String getValueSafely(TreeTableColumn.CellDataFeatures<AnalyzerRowModel, String> param, java.util.function.Function<AnalyzerRowModel, String> extractor) {
        AnalyzerRowModel model = getValueModel(param);
        if (model != null) {
            return extractor.apply(model);
        }
        return "";
    }
}
