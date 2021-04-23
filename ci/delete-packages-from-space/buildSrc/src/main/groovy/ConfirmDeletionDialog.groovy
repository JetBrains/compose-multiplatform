import groovy.swing.SwingBuilder
import javax.swing.*

class ConfirmDeletionDialog {
    static Boolean confirm(String packagesToBeDeleted) {
        def isConfirmed = false

        def sb = new SwingBuilder()
        sb.edt {
            dialog(
                modal: true,
                title: 'Confirm packages deletion',
                alwaysOnTop: true,
                size: [800, 600],
                resizable: false,
                locationRelativeTo: null,
                pack: true,
                show: true
            ) {
                vbox {
                    hbox() {
                        label(text: "Are you sure you want to delete these packages?")
                    }
                    scrollPane(verticalScrollBarPolicy: JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) {
                        textArea(text: packagesToBeDeleted, columns: 60, rows: 20)
                    }
                    hbox() {
                        button(text: 'Confirm', actionPerformed: {
                            isConfirmed = true
                            dispose()
                        })
                        button(text: 'Cancel', actionPerformed: {
                            isConfirmed = false
                            dispose()
                        })
                    }
                }
            }
        }
        sb.dispose()
        return isConfirmed
    }
}
