package za.co.tyaphile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Map;

public class OpenAccount {
    private JTextField name, surname;
    private JComboBox<String> accountType;
    private JButton cancel, submit;

    public OpenAccount(JFrame window) {
        init();

        JPanel main = new JPanel(new GridLayout(4, 2, 5, 5));
        main.add(new JLabel("Customer name: "));
        main.add(name);
        main.add(new JLabel("Customer surname: "));
        main.add(surname);
        main.add(new JLabel("Account type: "));
        main.add(accountType);
        main.add(cancel);
        main.add(submit);

        window.add(main);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setVisible(true);

        cancel.addActionListener(evt -> {
            accountType.setSelectedItem(accountType.getItemAt(0));
            window.dispose();
        });

        submit.addActionListener(evt -> {
            String fName = name.getText();
            String lName = surname.getText();
            String type = accountType.getItemAt(accountType.getSelectedIndex());

            if (fName.isBlank() || lName.isBlank()) {
                name.setText("");
                surname.setText("");
                accountType.setSelectedItem(accountType.getItemAt(0));
                JOptionPane.showMessageDialog(null, "Please fill in all details", "Incomplete", JOptionPane.WARNING_MESSAGE);
            } else {
                if (type.equals(accountType.getItemAt(0))) {
                    JOptionPane.showMessageDialog(null, "Invalid account type", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    try {
                        Map<String, Object> result = Client.sendRequest(Client.openAccount(fName, lName, type));
                        System.out.println(result);
                        JOptionPane.showMessageDialog(null, "Account opened", "Success", JOptionPane.INFORMATION_MESSAGE);

                        accountType.setSelectedItem(accountType.getItemAt(0));
                        window.dispose();
                    } catch (IOException | ClassNotFoundException e) {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        window.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                accountType.setSelectedItem(accountType.getItemAt(0));
                window.dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
    }

    private void init() {
        name = new JTextField();
        surname = new JTextField();
        accountType = new JComboBox<>();
        cancel = new JButton("Cancel");
        submit = new JButton("Submit");

        accountType.addItem("-- Select account type --");
        for (String type:Client.getAccountTypes()) {
            accountType.addItem(type);
        }
    }
}
