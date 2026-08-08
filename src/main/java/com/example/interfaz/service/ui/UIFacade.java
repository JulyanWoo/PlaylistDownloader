package com.example.interfaz.service.ui;

public class UIFacade {

    private final DialogService dialogService;
    private final NavigationService navigationService;
    private final ThemeService themeService;
    private final FolderChooserService folderChooserService;
    private final WindowManager windowManager;

    public UIFacade() {
        this.dialogService = new DialogService();
        this.navigationService = new NavigationService();
        this.themeService = new ThemeService();
        this.folderChooserService = new FolderChooserService();
        this.windowManager = new WindowManager(this.dialogService);
    }

    public UIFacade(
            DialogService dialogService,
            NavigationService navigationService,
            ThemeService themeService,
            FolderChooserService folderChooserService,
            WindowManager windowManager
    ) {
        this.dialogService = dialogService;
        this.navigationService = navigationService;
        this.themeService = themeService;
        this.folderChooserService = folderChooserService;
        this.windowManager = windowManager;
    }

    public DialogService getDialogService() {
        return dialogService;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public ThemeService getThemeService() {
        return themeService;
    }

    public FolderChooserService getFolderChooserService() {
        return folderChooserService;
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }
}
