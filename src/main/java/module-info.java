module diarsid.desktop.ui.components.calendar {

    requires org.slf4j;
    requires javafx.controls;
    requires diarsid.support;
    requires diarsid.support.javafx;

    exports diarsid.desktop.ui.components.calendar.api;
    exports diarsid.desktop.ui.components.calendar.api.defaultimpl;
}
