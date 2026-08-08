package com.example.interfaz.service.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class UIFacadeTest {

    private DialogService dialogService;
    private NavigationService navigationService;
    private ThemeService themeService;
    private FolderChooserService folderChooserService;
    private WindowManager windowManager;
    private UIFacade uiFacade;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        dialogService = new DialogService();
        navigationService = new NavigationService();
        themeService = new ThemeService();
        folderChooserService = new FolderChooserService();
        windowManager = new WindowManager(dialogService);

        uiFacade = new UIFacade(dialogService, navigationService, themeService, folderChooserService, windowManager);
    }

    @Test
    void testFacadeInitializationAndGetters() {
        assertNotNull(uiFacade.getDialogService());
        assertNotNull(uiFacade.getNavigationService());
        assertNotNull(uiFacade.getThemeService());
        assertNotNull(uiFacade.getFolderChooserService());
        assertNotNull(uiFacade.getWindowManager());

        assertSame(dialogService, uiFacade.getDialogService());
        assertSame(navigationService, uiFacade.getNavigationService());
        assertSame(themeService, uiFacade.getThemeService());
        assertSame(folderChooserService, uiFacade.getFolderChooserService());
        assertSame(windowManager, uiFacade.getWindowManager());
    }

    @Test
    void testDefaultConstructor() {
        UIFacade defaultFacade = new UIFacade();
        assertNotNull(defaultFacade.getDialogService());
        assertNotNull(defaultFacade.getNavigationService());
        assertNotNull(defaultFacade.getThemeService());
        assertNotNull(defaultFacade.getFolderChooserService());
        assertNotNull(defaultFacade.getWindowManager());
    }
}
