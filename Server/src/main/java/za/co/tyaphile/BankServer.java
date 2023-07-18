package za.co.tyaphile;


import za.co.tyaphile.database.Connector.Connect;
import za.co.tyaphile.database.DatabaseManager;
import za.co.tyaphile.info.Info;
import za.co.tyaphile.user.User;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BankServer implements Executor {

    private final Map<Integer, User> users = new HashMap<>();
    private int accountID = 0;
    private final int port = 5555;
    private boolean listen, activeComm;
    public static boolean MySQL;

    public static boolean isMySQL() {
        return MySQL;
    }

    BankServer() {
        setLookAndFeel();
        init();
//        execute(transactionSimulator());
        runServer();
    }

    private void runServer() {
        System.out.println("Starting server...");
        System.out.println("Server started...");
        System.out.println("Setting up server...");
        try {
            ServerSocket ss = new ServerSocket(port);
            listen = true;
            System.out.println("Server ready and listening for connections...");
            while (listen) {
                execute(new ListenSession(ss.accept()));
            }
        } catch (IOException e) {
            System.err.println("Setup error: " + e);
            for (StackTraceElement ste : e.getStackTrace()) {
                System.err.println("Setup error: " + ste);
            }
            listen = false;
        }
    }

    private class ListenSession implements Runnable {

        private final Socket soc;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public ListenSession(Socket socket) {
            this.soc = socket;
            activeComm = true;
        }

        @Override
        public void run() {}

//        public void run() {
//            System.out.println("Client ID: " + soc.getPort() + " connected...");
//            while (activeComm) {
//                try {
//                    ois = new ObjectInputStream(soc.getInputStream());
//                    Object obj = ois.readObject();
//                    System.out.println("Received class: " + obj.getClass());
//
//                    if(obj instanceof Transaction) {
//                        Transaction transact = (Transaction) obj;
//
//                        if(transact.isDeposit()) {
//                            sendObjects(DatabaseManager.setBalance(transact, transact.getAmount()), soc.getOutputStream());
//                        } else if (transact.isWithdrawal()) {
//                            sendObjects(DatabaseManager.setBalance(transact, -transact.getAmount()), soc.getOutputStream());
//                        } else if (transact.isPayment()) {
//                            sendObjects(DatabaseManager.makeTransaction(transact), soc.getOutputStream());
//                        } else {
//                            // Get all transactions with search criteria
//                            if(transact.isSearching()) {
//                                sendObjects(DatabaseManager.getTransactions(transact), soc.getOutputStream());
//                            } else {
//                                // Get all transactions without search criteria
//                                sendObjects(DatabaseManager.getTransactions(), soc.getOutputStream());
//                            }
//                        }
//                    }
//
//                    if(obj instanceof Account) {
//                        Account account = (Account) obj;
//
//                        if(account.isNewAccount()) {
//                            DatabaseManager.openAccount(account);
//                        } else if (account.isSearching()) {
//                            if(account.getSearch() == null) {
//                                sendObjects(DatabaseManager.getAccounts(), soc.getOutputStream());
//                            } else {
//                                sendObjects(DatabaseManager.getAccounts(account.getSearch(), account.getAccountType()), soc.getOutputStream());
//                            }
//                        } else if (account.isOnHold()) {
//                            boolean success = DatabaseManager.accountHold(account);
//                            sendObjects(success, soc.getOutputStream());
//                        } else if (account.isClosed()) {
//                            boolean success = DatabaseManager.accountClose(account);
//                            sendObjects(success, soc.getOutputStream());
//                        } else {
//                            boolean success = DatabaseManager.accountHold(account);
//                            sendObjects(success, soc.getOutputStream());
//                        }
//                    }
//
//                    if (obj instanceof User) {
//                        sendObjects(getUserInfo(), soc.getOutputStream());
//                    }
//
//                    if(obj instanceof Card) {
//                        Card card = (Card) obj;
//                        sendObjects(DatabaseManager.cardManager(card), soc.getOutputStream());
//                    }
//                } catch (IOException | ClassNotFoundException e) {
//                    System.err.println("Listen error: " + e);
//                    for (StackTraceElement ste : e.getStackTrace()) {
//                        System.err.println("Listen error: " + ste);
//                    }
//                    activeComm = false;
//                }
//            }
//        }
    }
//
//    private void stopAndIssueCard(za.co.tyaphile.card.Card card) {
//        for (Map.Entry<Integer, User> acc:users.entrySet()) {
//            if(acc.getValue().getLastCardIssued().formatCardNumber(acc.getValue().getLastCardIssued().getCardNumber()).equals(card.getCardNumber())) {
//                if (card.isIssue()) {
//                    acc.getValue().getLastCardIssued().setSTOP(true, card.getStopReason());
//                } else {
//                    acc.getValue().getLastCardIssued().setSTOP(card.isSTOP(), card.getStopReason());
//                }
//
//                acc.getValue().getLastCardIssued().setFRAUD(card.isFRAUD());
//                acc.getValue().issueCard();
//            }
//        }
//    }

//    private List<User> getUserInfo() {
//        List<User> usersAcc = new ArrayList<>();
//
//        for (Map.Entry<Integer, User> acc: users.entrySet()) {
//            Card card = acc.getValue().getLastCardIssued();
//            Account account = acc.getValue().getAccount();
//            List<za.co.tyaphile.card.Card> cards = new ArrayList<>();
//
//            for(Card c:acc.getValue().getAllCards()) {
//                za.co.tyaphile.card.Card card1;
//
//                if(c.isFRAUD() || c.isSTOPPED()) {
//                    card1 = new Card(c.formatCardNumber(c.getCardNumber()), c.getCardPin(), c.getCVV(),c.getStopReason(),  c.isSTOPPED(), c.isFRAUD());
//                } else {
//                    card1 = new Card(c.formatCardNumber(c.getCardNumber()), c.getCardPin(), c.getCVV());
//                }
//
//                for (String r:c.getNotes()) {
//                    card1.getNotes().add(r);
//                }
//
//                cards.add(card1);
//            }
//            za.co.tyaphile.account.Account account1 = new za.co.tyaphile.account.Account(account.getName(), account.getSurname(), account.getAccountNumber(), account.getAccountType());
//            account1.setBalance(account.getBalance().doubleValue());
//            account1.setClosed(account.isClosed(), account.getCloseReason());
//            account1.setOnHold(account.isOnHold(), account.getHoldReason());
//            account1.setOverDraft(account.getOverDraft().doubleValue());
//            account1.setOverDraftLimit(account.getOverDraftLimit().doubleValue());
//
//            for (String r:account.getNotes()) {
//                account1.getNotes().add(r);
//            }
//
//
//            Card card1 = new Card(card.formatCardNumber(card.getCardNumber()), card.getCardPin(), card.getCVV());
//
//            User user = new User(card1, account1, cards);
//            usersAcc.add(user);
//        }
//
//        return usersAcc;
//    }

    private void sendObjects(Object obj, OutputStream stream) {
        try {
            ObjectOutputStream outs = new ObjectOutputStream(stream);
            outs.writeObject(obj);
            System.out.println("Object + " + obj.getClass() + " sent...");
        } catch (IOException e) {
            System.err.println("Listen error: " + e);
            for (StackTraceElement ste : e.getStackTrace()) {
                System.err.println("Listen error: " + ste);
            }
        }
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
                 UnsupportedLookAndFeelException e1) {
            e1.printStackTrace();
        }

        Object[] options = {"MySQL", "SQLite", "Exit"};
        int opt = JOptionPane.showOptionDialog(null,
                "Set up database you would like to use\n\n" +
                        "NB: MySQL needs you to have the MySQL server installed on your system, SQLite requires no server set up",
                "Select database",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

        if(opt == JOptionPane.YES_OPTION) {
            MySQL = true;

            String user = JOptionPane.showInputDialog(null, "Enter database user: ", "User name", JOptionPane.QUESTION_MESSAGE);

            JPanel panel = new JPanel();
            JLabel label = new JLabel("Enter a password:");
            JPasswordField pass = new JPasswordField(10);
            panel.add(label);
            panel.add(pass);
            String[] opts = new String[]{"OK", "Cancel"};
            int option = JOptionPane.showOptionDialog(null, panel, "Password",
                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, opts, options[1]);
            if(option == 0) {
                char[] password = pass.getPassword();
                Info.setROOT(user);
                Info.setPASSWORD(String.valueOf(password));
            }
        } else if (opt == JOptionPane.NO_OPTION) {
            MySQL = false;
        } else {
            JOptionPane.showMessageDialog(null, "Closing application", "Close", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

//    private Runnable transactionSimulator() {
//        return () -> {
//            Scanner scanner = new Scanner(System.in);
//            String input, name, surname, accNo, amount, reason;
//
//            outer:
//            while (true) {
//                System.out.println("Enter A to add new Account\n" +
//                        "Enter C to close Account\n" +
//                        "Enter H to place Account on hold\n" +
//                        "Enter R to remove hold on Account\n" +
//                        "Enter T to transact on account");
//                input = scanner.nextLine();
//
//                User user;
//
//                switch (input.toUpperCase()) {
//                    case "A":
//                        System.out.print("Enter your First name: ");
//                        name = scanner.nextLine();
//                        System.out.print("Enter your surname: ");
//                        surname = scanner.nextLine();
//                        System.out.println("Enter \n\t1 for Personal account\n\t2 for Business account");
//                        input = scanner.nextLine();
//                        switch (input.trim()) {
//                            case "1":
//                                user = new User(name, surname, "Personal Account");
//                                user.issueCard();
//                                users.put(accountID++, user);
//                                break;
//                            case "2":
//                                user = new User(name, surname, "Business Account");
//                                user.issueCard();
//                                users.put(accountID++, user);
//                                break;
//                            default:
//                                System.err.println("You have entered incorrect options");
//                                break outer;
//                        }
//                        break;
//                    case "C":
//                        System.out.print("Enter account number to close: ");
//                        input = scanner.nextLine();
//                        System.out.print("Reason for closure: ");
//                        reason = scanner.nextLine();
//
//                        for (Map.Entry<Integer, User> acc: users.entrySet()) {
//                            if(String.valueOf(acc.getValue().getAccount().getAccountNumber()).trim().equals(input.trim())) {
//                                System.err.println("You are about to close account: " + input.trim() + "\n" +
//                                        "Account type: " + acc.getValue().getAccount().getAccountType() + "\n" +
//                                        "Name: " + acc.getValue().getAccount().getName() + ", Surname: " + acc.getValue().getAccount().getSurname());
//                                System.err.println("Are you sure you want to close this account?\nY for Yes or any key to cancel");
//                                input = scanner.nextLine();
//                                if (input.equalsIgnoreCase("Y")) {
//                                    acc.getValue().getAccount().setClosed(true, reason);
//                                    System.out.println("Account closed successfully");
//                                }
//                            }
//                        }
//                        break;
//                    case "H":
//                        System.out.print("Enter account number to place on hold: ");
//                        input = scanner.nextLine();
//                        System.out.print("Reason for hold: ");
//                        reason = scanner.nextLine();
//
//                        for (Map.Entry<Integer, User> acc: users.entrySet()) {
//                            if(String.valueOf(acc.getValue().getAccount().getAccountNumber()).trim().equals(input.trim())) {
//                                System.err.println("You are about to place account '" + input.trim() + "' on hold\n" +
//                                        "Account type: " + acc.getValue().getAccount().getAccountType() + "\n" +
//                                        "Name: " + acc.getValue().getAccount().getName() + ", Surname: " + acc.getValue().getAccount().getSurname());
//                                System.err.println("Are you sure you want to place hold on this account?\nY for Yes or any key to cancel");
//                                input = scanner.nextLine();
//                                if (input.equalsIgnoreCase("Y")) {
//                                    acc.getValue().getAccount().setOnHold(true, reason);
//                                    System.out.println("Account placed on hold successfully");
//                                }
//                            }
//                        }
//                        break;
//                    case "R":
//                        System.out.print("Enter account number to remove hold: ");
//                        input = scanner.nextLine();
//                        System.out.print("Reason for hold removal: ");
//                        reason = scanner.nextLine();
//
//                        for (Map.Entry<Integer, User> acc: users.entrySet()) {
//                            if(String.valueOf(acc.getValue().getAccount().getAccountNumber()).trim().equals(input.trim())) {
//                                System.err.println("You are about to remove hold on account '" + input.trim() + "'\n" +
//                                        "Account type: " + acc.getValue().getAccount().getAccountType() + "\n" +
//                                        "Name: " + acc.getValue().getAccount().getName() + ", Surname: " + acc.getValue().getAccount().getSurname());
//                                System.err.println("Are you sure you want to remove hold on this account?\nY for Yes or any key to cancel");
//                                input = scanner.nextLine();
//                                if (input.equalsIgnoreCase("Y")) {
//                                    acc.getValue().getAccount().setOnHold(false, reason);
//                                    System.out.println("Account hold removed successfully");
//                                }
//                            }
//                        }
//                        break;
//                    case "T":
//                        System.out.println("Find an account to transact");
//
//                        for(za.co.tyaphile.user.User user1:DatabaseManager.getAccounts("151745250", "Personal Account")) {
//                            System.out.println(user1.getAccount().getName());
//                        }
//
//                        System.out.println("Press P to make payments\n"
//                                + "Press D to make a deposit\n"
//                                + "Press W to make a withdrawal\n"
//                                + "Press F to stop old and issue new card because of fraud on client account\n"
//                                + "Press N to stop old and get a new card");
//
//                        input = scanner.nextLine();
//                        switch (input.trim().toUpperCase()) {
//                            case "P":
//                                System.out.print("Enter payer account number: ");
//                                String from = scanner.nextLine();
//                                System.out.print("Enter recipient account number: ");
//                                String to = scanner.nextLine();
//                                System.out.print("Enter amount to pay: ");
//                                amount = scanner.nextLine();
//
//                                int payer = -1, beneficiary = -1;
//                                User pay, rec;
//
//                                for (Map.Entry<Integer, User> acc: users.entrySet()) {
//                                    if (acc.getValue().getAccount().getAccountNumber().equals(from.trim())) {
//                                        payer = acc.getKey();
//                                    }
//                                    if (acc.getValue().getAccount().getAccountNumber().equals(to.trim())) {
//                                        beneficiary = acc.getKey();
//                                    }
//
//                                    if ((payer != -1) && (beneficiary != -1)) {
//                                        break;
//                                    }
//                                }
//
//                                if ((payer != -1) && (beneficiary != -1)) {
//                                    pay = users.get(payer);
//                                    rec = users.get(beneficiary);
//
//                                    if(pay.getAccount().isOnHold() || pay.getAccount().isClosed()) {
//                                        System.err.println("Unable to process request as there is a hold on account");
//                                    } else {
//                                        System.out.println("Payer has R " + pay.getAccount().getBalance() + " on account");
//                                        System.out.println("Recipient has R " + rec.getAccount().getBalance() + " on account");
//
//                                        if (pay.getAccount().getBalance().doubleValue() < Double.parseDouble(amount)) {
//                                            System.err.println(pay.getAccount().getAccountNumber() + " has insufficient funds to make paymenta");
//                                        } else {
//                                            pay.getAccount().setBalance(-(Double.parseDouble(amount)));
//                                            rec.getAccount().setBalance(Double.parseDouble(amount));
//
//                                            System.out.println("Payer has R " + pay.getAccount().getBalance() + " on account after payments");
//                                            System.out.println("Recipient has R " + rec.getAccount().getBalance() + " on account after payments");
//                                        }
//                                    }
//                                } else {
//                                    System.err.println("Error in finding accounts");
//                                }
//                                System.out.println();
//                                break ;
//                            case "D":
//                                System.out.print("Enter your account number: ");
//                                accNo = scanner.nextLine();
//                                for(Map.Entry<Integer, User> acc: users.entrySet()) {
//                                    if(acc.getValue().getAccount().getAccountNumber().equals(accNo)) {
//                                        if(acc.getValue().getAccount().isOnHold() || acc.getValue().getAccount().isClosed()) {
//                                            System.err.println("Unable to process request as there is a hold on account");
//                                        } else {
//                                            System.out.println("Current balance: R " + acc.getValue().getAccount().getBalance());
//                                            System.out.print("Enter deposit amount: ");
//                                            String value = scanner.nextLine();
//                                            acc.getValue().getAccount().setBalance(Double.parseDouble(value));
//                                            System.out.println("Available balance: R " + acc.getValue().getAccount().getBalance());
//                                        }
//                                    }
//                                }
//                                break;
//                            case "W":
//                                System.out.print("Enter your account number: ");
//                                accNo = scanner.nextLine();
//                                System.out.print("Enter Withdrawal amount: ");
//                                amount = scanner.nextLine();
//                                for(Map.Entry<Integer, User> acc: users.entrySet()) {
//                                    if(acc.getValue().getAccount().getAccountNumber().equals(accNo.trim())) {
//                                        if(acc.getValue().getAccount().isOnHold() || acc.getValue().getAccount().isClosed()) {
//                                            System.err.println("Unable to process request as there is a hold on account");
//                                        } else {
//                                            System.out.println("Before withdrawal amount: R" + acc.getValue().getAccount().getBalance());
//                                            acc.getValue().getAccount().setBalance(Double.parseDouble(amount));
//                                            System.out.println("Balance after withdrawal: R" + acc.getValue().getAccount().getBalance());
//                                        }
//                                    }
//                                }
//                                break;
//                            case "F":
//                                System.out.println("Enter C if fraud on Card or A if fraud on Acc");
//                                input = scanner.nextLine();
//                                switch (input.trim().toUpperCase()) {
//                                    case "A":
//                                        System.out.print("Enter account number: ");
//                                        accNo = scanner.nextLine();
//                                        System.out.print("Enter reason for stopping account: ");
//                                        input = scanner.nextLine();
//                                        for (Map.Entry<Integer, User> acc:users.entrySet()) {
//                                            if (acc.getValue().getAccount().getAccountNumber().equals(accNo.trim())) {
//                                                acc.getValue().getAccount().setOnHold(true, input);
//                                            }
//                                        }
//                                        break;
//                                    case "C":
//                                        System.out.print("Enter card number: ");
//                                        accNo = scanner.nextLine();
//                                        System.out.print("Enter reason for stopping card: ");
//                                        reason = scanner.nextLine();
//                                        for (Map.Entry<Integer, User> acc:users.entrySet()) {
//                                            if(acc.getValue().getLastCardIssued().formatCardNumber(acc.getValue().getLastCardIssued().getCardNumber()).equals(accNo.trim())) {
//                                                acc.getValue().getLastCardIssued().setFRAUD(true);
//                                                acc.getValue().getLastCardIssued().setSTOP(true, reason);
//                                                System.out.println(acc.getValue().getLastCardIssued().formatCardNumber(acc.getValue().getLastCardIssued().getCardNumber()) + " stopped");
//                                                acc.getValue().issueCard();
//                                                System.out.println(acc.getValue().getLastCardIssued().formatCardNumber(acc.getValue().getLastCardIssued().getCardNumber()) + " issued");
//                                            }
//                                        }
//                                        break;
//                                    default:
//                                        System.err.println("Incorrect selection");
//                                }
//                                break ;
//                            case "N":
//                                System.out.print("Enter account number: ");
//                                accNo = scanner.nextLine();
//                                System.out.print("Reason for new card request");
//                                reason = scanner.nextLine();
//                                for(Map.Entry<Integer, User> acc: users.entrySet()) {
//                                    if(acc.getValue().getAccount().getAccountNumber().equals(accNo.trim())) {
//                                        acc.getValue().getLastCardIssued().setSTOP(true, reason);
//                                        acc.getValue().issueCard();
//                                    }
//                                }
//                                break ;
//                            default:
//                                System.err.println("Unknown option: " + input.toUpperCase());
//                        }
//                        System.out.println();
//                        break;
//                    default:
//                        System.err.println("Incorrect selection");
//                        System.out.println();
//                        break outer;
//                }
//            }
//        };
//    }

    private void init() {
        DatabaseManager dbm = new DatabaseManager();
        if (MySQL) {
            Connect.createDatabase(Info.getDatabaseName(true), Info.getROOT(), Info.getPASSWORD());
        }
        DatabaseManager.createTables();
    }

    public static void main(String... args) { new BankServer(); }

    @Override
    public void execute(Runnable command) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(command);
    }
}
