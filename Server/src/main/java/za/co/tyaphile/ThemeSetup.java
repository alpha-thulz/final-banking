package za.co.tyaphile;

import za.co.tyaphile.info.Info;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeSetup {
    public ThemeSetup() {
        setLookAndFeel();
    }

    private void setLookAndFeel() {
        List<UIManager.LookAndFeelInfo> laf = new ArrayList<>();
        for (UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            laf.add(lafInfo);
        }

        int index = 1;
        try {
            UIManager.setLookAndFeel(laf.get(index).getClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            printStackTrace(e);
        }

        Object[] options = {"MySQL", "SQLite", "Exit"};
        int opt = JOptionPane.showOptionDialog(null,
                "Set up database you would like to use\n\n" +
                        "NB: MySQL needs you to have the MySQL server installed on your system, SQLite requires no server set up",
                "Select database",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

        if(opt == JOptionPane.YES_OPTION) {
            BankServer.MySQL = true;

            String user = JOptionPane.showInputDialog(null, "Enter database user: ",
                    "User name",JOptionPane.QUESTION_MESSAGE);
            if (user != null) {
                JPanel panel = new JPanel();
                JLabel label = new JLabel("Enter a password:");
                JPasswordField pass = new JPasswordField(10);
                panel.add(label);
                panel.add(pass);
                String[] opts = new String[]{"OK", "Cancel"};
                int option = JOptionPane.showOptionDialog(null, panel, "Password",
                        JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                        null, opts, options[1]);
                if (option == JOptionPane.YES_OPTION) {
                    char[] password = pass.getPassword();
                    Info.setROOT(user);
                    Info.setPASSWORD(String.valueOf(password));
                } else {
                    exit();
                }
            } else {
                exit();
            }
        } else if (opt == JOptionPane.NO_OPTION) {
            BankServer.MySQL = false;
        } else {
            exit();
        }
    }

    private void exit() {
        JOptionPane.showMessageDialog(null, "Closing application", "Close", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void printStackTrace(Exception e) {
        System.err.println("Look and feel error" + ": " + e.getMessage());
        for (StackTraceElement ste:e.getStackTrace()) {
            System.err.println(ste);
        }
    }
}
