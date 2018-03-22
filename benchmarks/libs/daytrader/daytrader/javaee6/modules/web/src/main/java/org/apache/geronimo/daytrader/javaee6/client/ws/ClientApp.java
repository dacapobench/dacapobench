
/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.daytrader.javaee6.client.ws;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.util.TimerTask;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JComboBox;
import javax.swing.JProgressBar;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.JMenuItem;
import javax.swing.border.EtchedBorder;
import javax.swing.border.BevelBorder;

public class ClientApp extends JFrame {

    private JPanel jContentPane = null;
    private JLabel jLabel = null;
    private JPanel jPanel = null;
    private JButton jButton = null;
    private JTextField jTextField = null;
    private JLabel jLabel1 = null;
    private JScrollPane jScrollPane = null;
    private JTextArea jTextArea = null; //
    private JLabel jLabel2 = null;
    private JPasswordField jPasswordField1 = null;
    private JLabel jLabel3 = null;
    private JPanel jPanel1 = null;
    private JPanel jPanel2 = null;
    private JPanel jPanel3 = null;
    private JPanel jPanel4 = null;
    private JPanel jPanel5 = null;
    private JPanel jPanel6 = null;
    private JButton jButton1 = null;
    private JPanel jPanel7 = null;
    private JPanel jPanel8 = null;
    private JButton jButton2 = null;
    private JButton jButton3 = null;
    private JButton jButton4 = null;
    private JButton jButton5 = null;
    private JButton jButton6 = null;
    private JButton jButton7 = null;
    private JButton jButton8 = null;
    private JLabel jLabel4 = null;
    private JLabel jLabel5 = null;
    private JLabel jLabel6 = null;
    private JTextField jTextField2 = null;
    private JTextField jTextField3 = null;
    private JTextField jTextField4 = null;
    private JLabel jLabel7 = null;
    private JTextField jTextField5 = null;
    private JPanel jPanel9 = null;
    private JPanel jPanel10 = null;
    private JLabel jLabel8 = null;
    private JLabel jLabel9 = null;
    private JComboBox jComboBox = null;
    private JTextField jTextField1 = null;
    private JTextField jTextField6 = null;
    private JTextField jTextField7 = null;
    private JButton jButton9 = null;
    private JLabel jLabel10 = null;
    private JLabel jLabel11 = null;
    private JLabel jLabel12 = null;
    private JLabel jLabel13 = null;
    private JLabel jLabel14 = null;
    private JLabel jLabel15 = null;
    private JLabel jLabel16 = null;
    private JLabel jLabel17 = null;
    private JLabel jLabel18 = null;
    private JLabel jLabel19 = null;
    private JLabel jLabel20 = null;
    private JLabel jLabel21 = null;
    private JLabel jLabel22 = null;
    private JLabel jLabel23 = null;
    private JLabel jLabel24 = null;
    private JLabel jLabel25 = null;
    private JLabel jLabel26 = null;
    private JProgressBar jProgressBar = null;
    private JButton jButton10 = null;
    private JPanel jPanel11 = null;
    
    

    public static void main(String[] args) {
        boolean waitForMain = false;
        
        try {
            if (args.length > 0) {
                if (args[0].equals("-waitForMain")) {
                    waitForMain = true;
                } else {
                    System.out.println("Usage ClientApp [-waitForMain]");
                    System.exit(1);
                }
            }
            
            ClientApp wsapp = new ClientApp();
        
            // Added the "waitForMain" flag to disable/enable the workaround below        
            if (waitForMain) {
                // Geronimo client terminates JVM process when Main completes (not sure why)
                // even though client GUI is still active. For now, force Main to remain alive
                // until GUI is closed.
                
                while (wsapp.isVisible())
                    Thread.sleep(5000);
            }
        } catch (Exception e) {
            System.err.println("Caught an unexpected exception!");
            e.printStackTrace();
        }
    }

    public ClientApp() {
        super();
        initialize();
    }

    private void initialize() {
        this.setContentPane(getJContentPane());
        this.setSize(673, 577);
        this.setVisible(true);
        this.setTitle("Web Services JAX RPC Client");
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }
    /**
     * This method initializes jContentPane
     *
     * @return JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(getJLabel(), null);
            jContentPane.add(getJPanel(), null);
            jContentPane.add(getJScrollPane(), null);
            jContentPane.add(getJLabel3(), null);
            jContentPane.add(getJPanel1(), null);
            jContentPane.add(getJPanel2(), null);
            jContentPane.add(getJPanel3(), null);
            jContentPane.add(getJPanel4(), null);
            jContentPane.add(getJPanel5(), null);
            jContentPane.add(getJPanel6(), null);
            jContentPane.add(getJPanel7(), null);
            jContentPane.add(getJPanel8(), null);
            jContentPane.add(getJPanel11(), null);
            jContentPane.setBackground(new java.awt.Color(244, 245, 245));
        }
        return jContentPane;
    }
    /**
     * This method initializes jLabel
     *
     * @return JLabel
     */
    private JLabel getJLabel() {
        if (jLabel == null) {
            URL imageUrl = getClass().getResource("/images/dayTraderLogo.gif");
            if (imageUrl == null) {
                throw new IllegalStateException("Cannont locate daytraderLogo.gif");
            }
            ImageIcon imageIcon = new ImageIcon(imageUrl);
            jLabel = new JLabel(imageIcon);
            jLabel.setBounds(11, 7, 193, 71);
			jLabel.setText("");
        }
        return jLabel;
    }
    /**
     * This method initializes jPanel
     *
     * @return JPanel
     */
    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(null);
            jPanel.add(getJButton(), getJButton().getName());
            jPanel.add(getJTextField(), null);
            jPanel.add(getJLabel1(), null);
            jPanel.add(getJLabel2(), null);
            jPanel.add(getJTextField1(), null);
            jPanel.setBounds(11, 83, 287, 53);
            jPanel.setBackground(new java.awt.Color(245, 245, 245));
            jPanel.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel;
    }

    /**
     * This method initializes jButton
     *
     * @return JButton
     */
    private JButton getJButton() {
        if (jButton == null) {
            jButton = new JButton();
            jButton.setBounds(9, 8, 68, 25);
            jButton.setText("Login");
            jButton.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton.setBackground(new java.awt.Color(220, 220, 220));
            jButton.addActionListener(new AppActionListener());
        }
        return jButton;
    }
    /**
     * This method initializes jTextField
     *
     * @return JTextField
     */
    private JTextField getJTextField() {
        if (jTextField == null) {
            jTextField = new JTextField();
            jTextField.setBounds(182, 6, 93, 19);
            jTextField.setToolTipText("UserName");
            jTextField.setText("uid:0");
            jTextField.setName("UserName");
            jTextField.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jTextField;
    }
    /**
     * This method initializes jLabel1
     *
     * @return JLabel
     */
    private JLabel getJLabel1() {
        if (jLabel1 == null) {
            jLabel1 = new JLabel();
            jLabel1.setBounds(91, 7, 82, 15);
            jLabel1.setText("Username");
            jLabel1.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel1;
    }
    /**
     * This method initializes jScrollPane
     *
     * @return JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTextArea());
            jScrollPane.setBounds(310, 8, 351, 255);
            jScrollPane.setBorder(
                BorderFactory.createTitledBorder(
                    null,
                    "Results",
                    0,
                    0));
            jScrollPane.setToolTipText("Result Output");
            jScrollPane.setName("ResultPane");
        }
        return jScrollPane;
    }
    /**
     * This method initializes jTextArea
     *
     * @return JTextArea
     */
    private JTextArea getJTextArea() {
        if (jTextArea == null) {
            jTextArea = new JTextArea();
            jTextArea.setText("");
            jTextArea.setFont(new java.awt.Font("sansserif", 0, 10));
            //provide a generic output variable to allow the output source to be chagned easily
            output = jTextArea;
        }
        return jTextArea;
    }
    /**
     * This method initializes jLabel2
     *
     * @return JLabel
     */
    private JLabel getJLabel2() {
        if (jLabel2 == null) {
            jLabel2 = new JLabel();
            jLabel2.setSize(82, 15);
            jLabel2.setText("Password");
            jLabel2.setLocation(90, 27);
            jLabel2.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel2.setToolTipText("Trade User's password");
            jLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel2;
    }
    /**
     * This method initializes jTextField1
     *
     * @return JTextField
     */
    private JTextField getJTextField1() {
        if (jPasswordField1 == null) {
            jPasswordField1 = new JPasswordField();
            jPasswordField1.setSize(94, 19);
            jPasswordField1.setLocation(181, 29);
            jPasswordField1.setFont(new java.awt.Font("sansserif", 0, 10));
            jPasswordField1.setPreferredSize(new java.awt.Dimension(4, 15));
            jPasswordField1.setText("xxx");
        }
        return jPasswordField1;
    }
    /**
     * This method initializes jLabel3
     *
     * @return JLabel
     */
    private JLabel getJLabel3() {
        if (jLabel3 == null) {
            jLabel3 = new JLabel();
            jLabel3.setBounds(212, 13, 87, 63);
            jLabel3.setText(
                "<html> Web Services Benchmark Scenario Application");
            jLabel3.setAutoscrolls(true);
            jLabel3.setFont(new java.awt.Font("sansserif", 1, 12));
            jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        }
        return jLabel3;
    }
    /**
     * This method initializes jPanel1
     *
     * @return JPanel
     */
    private JPanel getJPanel1() {
        if (jPanel1 == null) {
            jPanel1 = new JPanel();
            jPanel1.setLayout(null);
            jPanel1.add(getJButton1(), getJButton1().getName());
            jPanel1.setBounds(11, 142, 287, 38);
            jPanel1.setBackground(new java.awt.Color(245, 245, 245));
            jPanel1.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel1;
    }
    /**
     * This method initializes jPanel2
     *
     * @return JPanel
     */
    private JPanel getJPanel2() {
        if (jPanel2 == null) {
            jPanel2 = new JPanel();
            jPanel2.setLayout(null);
            jPanel2.add(getJButton2(), getJButton2().getName());
            jPanel2.add(getJLabel4(), null);
            jPanel2.add(getJTextField2(), null);
            jPanel2.setBounds(11, 185, 287, 38);
            jPanel2.setBackground(new java.awt.Color(245, 245, 245));
            jPanel2.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));

        }
        return jPanel2;
    }
    /**
     * This method initializes jPanel3
     *
     * @return JPanel
     */
    private JPanel getJPanel3() {
        if (jPanel3 == null) {
            jPanel3 = new JPanel();
            jPanel3.setLayout(null);
            jPanel3.add(getJButton3(), null);
            jPanel3.setBounds(11, 228, 287, 38);
            jPanel3.setBackground(new java.awt.Color(245, 245, 245));
            jPanel3.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel3;
    }
    /**
     * This method initializes jPanel4
     *
     * @return JPanel
     */
    private JPanel getJPanel4() {
        if (jPanel4 == null) {
            jPanel4 = new JPanel();
            jPanel4.setLayout(null);
            jPanel4.add(getJButton4(), null);
            jPanel4.add(getJLabel5(), null);
            jPanel4.add(getJLabel6(), null);
            jPanel4.add(getJTextField3(), null);
            jPanel4.add(getJTextField4(), null);
            jPanel4.setBounds(11, 271, 287, 53);
            jPanel4.setBackground(new java.awt.Color(245, 245, 245));
            jPanel4.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel4;
    }
    /**
     * This method initializes jPanel5
     *
     * @return JPanel
     */
    private JPanel getJPanel5() {
        if (jPanel5 == null) {
            jPanel5 = new JPanel();
            jPanel5.setLayout(null);
            jPanel5.add(getJButton5(), null);
            jPanel5.add(getJLabel7(), null);
            jPanel5.add(getJTextField5(), null);
            jPanel5.setBounds(11, 329, 287, 38);
            jPanel5.setBackground(new java.awt.Color(245, 245, 245));
            jPanel5.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel5;
    }
    /**
     * This method initializes jPanel6
     *
     * @return JPanel
     */
    private JPanel getJPanel6() {
        if (jPanel6 == null) {
            jPanel6 = new JPanel();
            jPanel6.setLayout(null);
            jPanel6.add(getJButton6(), null);
            jPanel6.setBounds(11, 373, 287, 38);
            jPanel6.setBackground(new java.awt.Color(245, 245, 245));
            jPanel6.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel6;
    }
    /**
     * This method initializes jButton1
     *
     * @return JButton
     */
    private JButton getJButton1() {
        if (jButton1 == null) {
            jButton1 = new JButton();
            jButton1.setSize(141, 25);
            jButton1.setLocation(14, 8);
            jButton1.setText("Market Summary");
            jButton1.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton1.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton1.setBackground(new java.awt.Color(220, 220, 220));
            jButton1.setActionCommand("MarketSummary");
            jButton1.addActionListener(new AppActionListener());
        }
        return jButton1;
    }
    /**
     * This method initializes jPanel7
     *
     * @return JPanel
     */
    private JPanel getJPanel7() {
        if (jPanel7 == null) {
            jPanel7 = new JPanel();
            jPanel7.setLayout(null);
            jPanel7.add(getJButton7(), null);
            jPanel7.setBounds(11, 417, 287, 38);
            jPanel7.setBackground(new java.awt.Color(245, 245, 245));
            jPanel7.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }
        return jPanel7;
    }
    /**
     * This method initializes jPanel8
     *
     * @return JPanel
     */
    private JPanel getJPanel8() {
        if (jPanel8 == null) {
            jPanel8 = new JPanel();
            jPanel8.setLayout(null);
            jPanel8.add(getJButton8(), null);
            jPanel8.setBounds(11, 462, 287, 38);
            jPanel8.setBackground(new java.awt.Color(245, 245, 245));
            jPanel8.setBorder(
                BorderFactory.createEtchedBorder(
                    EtchedBorder.LOWERED));
        }

        return jPanel8;
    }
    /**
     * This method initializes jButton2
     *
     * @return JButton
     */
    private JButton getJButton2() {
        if (jButton2 == null) {
            jButton2 = new JButton();
            jButton2.setBounds(9, 8, 68, 25);
            jButton2.setText("Quotes");
            jButton2.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton2.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton2.setBackground(new java.awt.Color(220, 220, 220));
            jButton2.addActionListener(new AppActionListener());
        }
        return jButton2;
    }
    /**
     * This method initializes jButton3
     *
     * @return JButton
     */
    private JButton getJButton3() {
        if (jButton3 == null) {
            jButton3 = new JButton();
            jButton3.setSize(141, 25);
            jButton3.setLocation(14, 8);
            jButton3.setText("Account Info");
            jButton3.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton3.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton3.setBackground(new java.awt.Color(220, 220, 220));
            jButton3.setActionCommand("Account");
            jButton3.addActionListener(new AppActionListener());
        }
        return jButton3;
    }
    /**
     * This method initializes jButton4
     *
     * @return JButton
     */
    private JButton getJButton4() {
        if (jButton4 == null) {
            jButton4 = new JButton();
            jButton4.setBounds(9, 8, 68, 25);
            jButton4.setText("Buy");
            jButton4.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton4.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton4.setBackground(new java.awt.Color(220, 220, 220));
            jButton4.addActionListener(new AppActionListener());
        }
        return jButton4;
    }
    /**
     * This method initializes jButton5
     *
     * @return JButton
     */
    private JButton getJButton5() {
        if (jButton5 == null) {
            jButton5 = new JButton();
            jButton5.setBounds(9, 8, 68, 25);
            jButton5.setText("Sell");
            jButton5.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton5.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton5.setBackground(new java.awt.Color(220, 220, 220));
            jButton5.addActionListener(new AppActionListener());
        }
        return jButton5;
    }
    /**
     * This method initializes jButton6
     *
     * @return JButton
     */
    private JButton getJButton6() {
        if (jButton6 == null) {
            jButton6 = new JButton();
            jButton6.setSize(141, 25);
            jButton6.setLocation(14, 8);
            jButton6.setText("View Orders");
            jButton6.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton6.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton6.setBackground(new java.awt.Color(220, 220, 220));
            jButton6.setActionCommand("Orders");
            jButton6.addActionListener(new AppActionListener());
        }
        return jButton6;
    }
    /**
     * This method initializes jButton7
     *
     * @return JButton
     */
    private JButton getJButton7() {
        if (jButton7 == null) {
            jButton7 = new JButton();
            jButton7.setSize(141, 25);
            jButton7.setLocation(14, 8);
            jButton7.setText("View Holdings");
            jButton7.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton7.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton7.setBackground(new java.awt.Color(220, 220, 220));
            jButton7.setActionCommand("Holdings");
            jButton7.addActionListener(new AppActionListener());
        }
        return jButton7;
    }
    /**
     * This method initializes jButton8
     *
     * @return JButton
     */
    private JButton getJButton8() {
        if (jButton8 == null) {
            jButton8 = new JButton();
            jButton8.setSize(141, 25);
            jButton8.setLocation(14, 8);
            jButton8.setText("Logout");
            jButton8.setFont(new java.awt.Font("sansserif", 1, 10));
            jButton8.setBorder(
                BorderFactory.createBevelBorder(
                    BevelBorder.RAISED));
            jButton8.setBackground(new java.awt.Color(220, 220, 220));
            jButton8.addActionListener(new AppActionListener());
        }
        return jButton8;
    }
    /**
     * This method initializes jLabel4
     *
     * @return JLabel
     */
    private JLabel getJLabel4() {
        if (jLabel4 == null) {
            jLabel4 = new JLabel();
            jLabel4.setBounds(91, 7, 82, 15);
            jLabel4.setText("Symbols");
            jLabel4.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel4;
    }
    /**
     * This method initializes jLabel5
     *
     * @return JLabel
     */
    private JLabel getJLabel5() {
        if (jLabel5 == null) {
            jLabel5 = new JLabel();
            jLabel5.setBounds(91, 7, 82, 15);
            jLabel5.setText("Symbol");
            jLabel5.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel5.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel5;
    }
    /**
     * This method initializes jLabel6
     *
     * @return JLabel
     */
    private JLabel getJLabel6() {
        if (jLabel6 == null) {
            jLabel6 = new JLabel();
            jLabel6.setSize(82, 15);
            jLabel6.setText("Quantity");
            jLabel6.setLocation(90, 27);
            jLabel6.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel6.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel6;
    }
    /**
     * This method initializes jTextField2
     *
     * @return JTextField
     */
    private JTextField getJTextField2() {
        if (jTextField2 == null) {
            jTextField2 = new JTextField();
            jTextField2.setBounds(182, 6, 93, 19);
            jTextField2.setToolTipText("Stock symbols");
            jTextField2.setText("s:1");
            jTextField2.setName("Symbols");
            jTextField2.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jTextField2;
    }
    /**
     * This method initializes jTextField3
     *
     * @return JTextField
     */
    private JTextField getJTextField3() {
        if (jTextField3 == null) {
            jTextField3 = new JTextField();
            jTextField3.setBounds(182, 6, 93, 19);
            jTextField3.setToolTipText("Stock symbols");
            jTextField3.setText("s:1");
            jTextField3.setName("symbol");
            jTextField3.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jTextField3;
    }
    /**
     * This method initializes jTextField4
     *
     * @return JTextField
     */
    private JTextField getJTextField4() {
        if (jTextField4 == null) {
            jTextField4 = new JTextField();
            jTextField4.setBounds(182, 27, 93, 19);
            jTextField4.setToolTipText("Quantity of shares to purchase");
            jTextField4.setText("100");
            jTextField4.setName("quantity");
            jTextField4.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jTextField4;
    }
    /**
     * This method initializes jLabel7
     *
     * @return JLabel
     */
    private JLabel getJLabel7() {
        if (jLabel7 == null) {
            jLabel7 = new JLabel();
            jLabel7.setBounds(91, 7, 82, 15);
            jLabel7.setText("Holding ID");
            jLabel7.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel7.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel7;
    }
    /**
     * This method initializes jTextField5
     *
     * @return JTextField
     */
    private JTextField getJTextField5() {
        if (jTextField5 == null) {
            jTextField5 = new JTextField();
            jTextField5.setBounds(182, 6, 93, 19);
            jTextField5.setToolTipText("Holding ID of stock to sell");
            jTextField5.setText("0");
            jTextField5.setName("holdingID");
            jTextField5.setFont(new java.awt.Font("Arial", 0, 10));
        }
        return jTextField5;
    }
    /**
     * This method initializes jPanel9
     *
     * @return JPanel
     */
    private JPanel getJPanel9() {
        if (jPanel9 == null) {
            jPanel9 = new JPanel();
            jPanel9.setLayout(null);
            jPanel9.add(getJLabel8(), null);
            jPanel9.add(getJLabel9(), null);
            jPanel9.add(getJComboBox(), null);
            jPanel9.add(getJTextField7(), null);
            jPanel9.add(getJTextField6(), null);
            jPanel9.add(getJButton9(), null);
            jPanel9.add(getJLabel27(), null);
            jPanel9.add(getJTextField12(), null);
            jPanel9.setBounds(11, 15, 347, 89);
            jPanel9.setBackground(new java.awt.Color(245, 245, 245));
            jPanel9.setBorder(
                BorderFactory.createTitledBorder(
                    null,
                    "Scenario Setup",
                    0,
                    0));
            jPanel9.setName("jPanel9");
        }
        return jPanel9;
    }
    /**
     * This method initializes jPanel10
     *
     * @return JPanel
     */
    private JPanel getJPanel10() {
        if (jPanel10 == null) {
            jPanel10 = new JPanel();
            jPanel10.setLayout(null);
            jPanel10.add(getJLabel10(), null);
            jPanel10.add(getJLabel11(), null);
            jPanel10.add(getJLabel12(), null);
            jPanel10.add(getJLabel13(), null);
            jPanel10.add(getJLabel14(), null);
            jPanel10.add(getJLabel15(), null);
            jPanel10.add(getJLabel16(), null);
            jPanel10.add(getJLabel17(), null);
            jPanel10.add(getJLabel18(), null);
            jPanel10.add(getJLabel19(), null);
            jPanel10.add(getJLabel20(), null);
            jPanel10.add(getJLabel21(), null);
            jPanel10.add(getJLabel22(), null);
            jPanel10.add(getJLabel23(), null);
            jPanel10.add(getJLabel24(), null);
            jPanel10.add(getJLabel25(), null);
            jPanel10.add(getJLabel26(), null);
            jPanel10.add(getJProgressBar(), null);
            jPanel10.add(getJButton10(), null);
            jPanel10.setBounds(9, 106, 347, 153);
            jPanel10.setBackground(new java.awt.Color(245, 245, 245));
            jPanel10.setBorder(
                BorderFactory.createTitledBorder(
                    null,
                    "Scenario Statistics",
                    0,
                    0));
            jPanel10.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jPanel10;
    }
    /**
     * This method initializes jLabel8
     *
     * @return JLabel
     */
    private JLabel getJLabel8() {
        if (jLabel8 == null) {
            jLabel8 = new JLabel();
            jLabel8.setBounds(11, 43, 59, 13);
            jLabel8.setText("#Threads");
            jLabel8.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel8.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel8;
    }
    /**
     * This method initializes jLabel9
     *
     * @return JLabel
     */
    private JLabel getJLabel9() {
        if (jLabel9 == null) {
            jLabel9 = new JLabel();
            jLabel9.setBounds(11, 65, 59, 13);
            jLabel9.setText("Iterations");
            jLabel9.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel9.setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return jLabel9;
    }
    //  @jve:visual-info  decl-index=0 visual-constraint="0,0"
    /**
     * This method initializes jComboBox
     *
     * @return JComboBox
     */
    private JComboBox getJComboBox() {
        if (jComboBox == null) {
            jComboBox = new JComboBox();
            jComboBox.setBounds(166, 41, 161, 16);
            jComboBox.setBackground(new java.awt.Color(220, 220, 220));
            jComboBox.setFont(new java.awt.Font("Arial", 1, 10));
            jComboBox.addItem("Get Quote");
            jComboBox.addItem("Get Quote - NULL");
            // TODO more scenarios here
            // jComboBox.addItem("Holdings Only");
        }
        return jComboBox;
    }
    /**
     * This method initializes jTextField1
     *
     * @return JTextField
     */
    private JTextField getJTextField7() {
        if (jTextField7 == null) {
            jTextField7 = new JTextField();
            jTextField7.setBounds(76, 43, 77, 13);
            jTextField7.setText("10");
            jTextField7.setFont(new java.awt.Font("Arial", 1, 10));
            jTextField7.setHorizontalAlignment(
                SwingConstants.RIGHT);
        }
        return jTextField7;
    }
    /**
     * This method initializes jTextField6
     *
     * @return JTextField
     */
    private JTextField getJTextField6() {
        if (jTextField6 == null) {
            jTextField6 = new JTextField();
            jTextField6.setBounds(79, 66, 77, 13);
            jTextField6.setText("10000");
            jTextField6.setFont(new java.awt.Font("Arial", 1, 10));
            jTextField6.setHorizontalAlignment(
                SwingConstants.RIGHT);
        }
        return jTextField6;
    }
    /**
     * This method initializes jButton9
     *
     * @return JButton
     */
    private JButton getJButton9() {
        if (jButton9 == null) {
            jButton9 = new JButton();
            jButton9.setBounds(169, 66, 149, 15);
            jButton9.setBackground(new java.awt.Color(220, 220, 220));
            jButton9.setFont(new java.awt.Font("Arial", 1, 10));
            jButton9.setText("Start Scenario!");
            jButton9.setActionCommand("start");
            jButton9.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String command = e.getActionCommand();
                    if (command.equalsIgnoreCase("start")) {
                        int numThreads = Integer.parseInt(jTextField7.getText());
                        int reqPerThread =
                            Integer.parseInt(jTextField6.getText())
                                / numThreads;
                        startScenario(numThreads, reqPerThread);
                        jButton9.setText("Stop");
                        jButton9.setActionCommand("stop");
                    } else if (command.equalsIgnoreCase("stop")) {
                        stopScenario();
                        jButton9.setText("Start Scenario!");
                        jButton9.setActionCommand("start");

                    }
                }
            });

        }
        return jButton9;
    }

    /**
     * This method initializes jLabel10
     *
     * @return JLabel
     */
    private JLabel getJLabel10() {
        if (jLabel10 == null) {
            jLabel10 = new JLabel();
            jLabel10.setBounds(11, 25, 106, 13);
            jLabel10.setText("Avg. Req/Sec");
            jLabel10.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel10.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel10.setToolTipText("The average requests per second");
        }
        return jLabel10;
    }
    /**
     * This method initializes jLabel11
     *
     * @return JLabel
     */
    private JLabel getJLabel11() {
        if (jLabel11 == null) {
            jLabel11 = new JLabel();
            jLabel11.setBounds(11, 50, 106, 13);
            jLabel11.setText("Current Req/Sec");
            jLabel11.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel11.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel11.setToolTipText(
                "The average request per second in the last 5 second interval");
        }
        return jLabel11;
    }
    /**
     * This method initializes jLabel12
     *
     * @return JLabel
     */
    private JLabel getJLabel12() {
        if (jLabel12 == null) {
            jLabel12 = new JLabel();
            jLabel12.setBounds(11, 75, 106, 13);
            jLabel12.setText("Total Requests");
            jLabel12.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel12.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel12.setToolTipText(
                "The total number of requests since the beginning of the scenario");
        }
        return jLabel12;
    }
    /**
     * This method initializes jLabel13
     *
     * @return JLabel
     */
    private JLabel getJLabel13() {
        if (jLabel13 == null) {
            jLabel13 = new JLabel();
            jLabel13.setBounds(11, 95, 106, 13);
            jLabel13.setText("Total Errors");
            jLabel13.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel13.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel13.setToolTipText(
                "The total number of errors since the beginning of the scenario");
        }
        return jLabel13;
    }
    /**
     * This method initializes jLabel14
     *
     * @return JLabel
     */
    private JLabel getJLabel14() {
        if (jLabel14 == null) {
            jLabel14 = new JLabel();
            jLabel14.setBounds(125, 25, 57, 15);
            jLabel14.setText("0.0");
            jLabel14.setBackground(java.awt.Color.white);
            jLabel14.setForeground(java.awt.Color.black);
            jLabel14.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel14;
    }
    /**
     * This method initializes jLabel15
     *
     * @return JLabel
     */
    private JLabel getJLabel15() {
        if (jLabel15 == null) {
            jLabel15 = new JLabel();
            jLabel15.setBounds(125, 50, 57, 15);
            jLabel15.setText("0.0");
            jLabel15.setBackground(java.awt.Color.white);
            jLabel15.setForeground(java.awt.Color.black);
            jLabel15.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel15;
    }
    /**
     * This method initializes jLabel16
     *
     * @return JLabel
     */
    private JLabel getJLabel16() {
        if (jLabel16 == null) {
            jLabel16 = new JLabel();
            jLabel16.setBounds(125, 75, 57, 15);
            jLabel16.setText("0");
            jLabel16.setBackground(java.awt.Color.white);
            jLabel16.setForeground(java.awt.Color.black);
            jLabel16.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel16;
    }
    /**
     * This method initializes jLabel17
     *
     * @return JLabel
     */
    private JLabel getJLabel17() {
        if (jLabel17 == null) {
            jLabel17 = new JLabel();
            jLabel17.setBounds(125, 95, 57, 15);
            jLabel17.setText("0");
            jLabel17.setBackground(java.awt.Color.white);
            jLabel17.setForeground(java.awt.Color.black);
            jLabel17.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel17;
    }
    /**
     * This method initializes jLabel18
     *
     * @return JLabel
     */
    private JLabel getJLabel18() {
        if (jLabel18 == null) {
            jLabel18 = new JLabel();
            jLabel18.setBounds(189, 25, 100, 13);
            jLabel18.setText("Avg. Resp. Time(ms)");
            jLabel18.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel18.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel18.setToolTipText("The average response time for requests");
        }
        return jLabel18;
    }
    /**
     * This method initializes jLabel19
     *
     * @return JLabel
     */
    private JLabel getJLabel19() {
        if (jLabel19 == null) {
            jLabel19 = new JLabel();
            jLabel19.setBounds(189, 50, 100, 13);
            jLabel19.setText("Min. Resp. Time(ms)");
            jLabel19.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel19.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel19.setToolTipText(
                "The minimum response time for all requests");
        }
        return jLabel19;
    }
    /**
     * This method initializes jLabel20
     *
     * @return JLabel
     */
    private JLabel getJLabel20() {
        if (jLabel20 == null) {
            jLabel20 = new JLabel();
            jLabel20.setBounds(189, 75, 100, 13);
            jLabel20.setText("Max. Resp. Time(ms)");
            jLabel20.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel20.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel20.setToolTipText(
                "The maximum response time for all requests");
        }
        return jLabel20;
    }
    /**
     * This method initializes jLabel21
     *
     * @return JLabel
     */
    private JLabel getJLabel21() {
        if (jLabel21 == null) {
            jLabel21 = new JLabel();
            jLabel21.setBounds(189, 95, 95, 13);
            jLabel21.setText("Total Time (secs)");
            jLabel21.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel21.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel21.setToolTipText(
                "The time in seconds since the beginning of the run");
        }
        return jLabel21;
    }
    /**
     * This method initializes jLabel22
     *
     * @return JLabel
     */
    private JLabel getJLabel22() {
        if (jLabel22 == null) {
            jLabel22 = new JLabel();
            jLabel22.setBounds(293, 25, 50, 15);
            jLabel22.setText("0.00");
            jLabel22.setBackground(java.awt.Color.white);
            jLabel22.setForeground(java.awt.Color.black);
            jLabel22.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel22;
    }
    /**
     * This method initializes jLabel23
     *
     * @return JLabel
     */
    private JLabel getJLabel23() {
        if (jLabel23 == null) {
            jLabel23 = new JLabel();
            jLabel23.setBounds(293, 50, 50, 15);
            jLabel23.setText("0.00");
            jLabel23.setBackground(java.awt.Color.white);
            jLabel23.setForeground(java.awt.Color.black);
            jLabel23.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel23;
    }
    /**
     * This method initializes jLabel24
     *
     * @return JLabel
     */
    private JLabel getJLabel24() {
        if (jLabel24 == null) {
            jLabel24 = new JLabel();
            jLabel24.setBounds(293, 75, 50, 15);
            jLabel24.setText("0.00");
            jLabel24.setBackground(java.awt.Color.white);
            jLabel24.setForeground(java.awt.Color.black);
            jLabel24.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel24;
    }
    /**
     * This method initializes jLabel25
     *
     * @return JLabel
     */
    private JLabel getJLabel25() {
        if (jLabel25 == null) {
            jLabel25 = new JLabel();
            jLabel25.setBounds(293, 95, 50, 15);
            jLabel25.setText("0");
            jLabel25.setBackground(java.awt.Color.white);
            jLabel25.setForeground(java.awt.Color.black);
            jLabel25.setFont(new java.awt.Font("sansserif", 0, 10));
        }
        return jLabel25;
    }
    /**
     * This method initializes jLabel26
     *
     * @return JLabel
     */
    private JLabel getJLabel26() {
        if (jLabel26 == null) {
            jLabel26 = new JLabel();
            jLabel26.setBounds(12, 127, 98, 13);
            jLabel26.setText("Scenario Progress");
            jLabel26.setFont(new java.awt.Font("Arial", 1, 10));
            jLabel26.setHorizontalAlignment(SwingConstants.RIGHT);
            jLabel26.setToolTipText(
                "The total number of errors since the beginning of the scenario");
        }
        return jLabel26;
    }
    /**
     * This method initializes jProgressBar
     *
     * @return JProgressBar
     */
    private JProgressBar getJProgressBar() {
        if (jProgressBar == null) {
            jProgressBar = new JProgressBar();
            jProgressBar.setBounds(117, 126, 145, 15);
            jProgressBar.setValue(0);
            jProgressBar.setBackground(new java.awt.Color(220, 220, 220));
        }
        return jProgressBar;
    }
    /**
     * This method initializes jButton10
     *
     * @return JButton
     */
    private JButton getJButton10() {
        if (jButton10 == null) {
            jButton10 = new JButton();
            jButton10.setBounds(272, 125, 66, 16);
            jButton10.setBackground(new java.awt.Color(220, 220, 220));
            jButton10.setFont(new java.awt.Font("Arial", 1, 10));
            jButton10.setText("Clear");
            jButton10.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    clearScenarioStats();
                    output.append("\n\tScenario: Cleared statistics");
                }
            });
        }
        return jButton10;
    }
    //  @jve:visual-info  decl-index=0 visual-constraint="0,0"
    /**
     * This method initializes jPanel11
     *
     * @return JPanel
     */
    private JPanel getJPanel11() {
        if (jPanel11 == null) {
            jPanel11 = new JPanel();
            jPanel11.setLayout(null);
            jPanel11.add(getJPanel9(), getJPanel9().getName());
            jPanel11.add(getJPanel9(), null);
            jPanel11.add(getJPanel10(), null);
            jPanel11.setBounds(302, 262, 361, 262);
            jPanel11.setBackground(new java.awt.Color(208, 208, 205));
            jPanel11.setBorder(
                BorderFactory.createTitledBorder(
                    null,
                    "Benchmark Scenario",
                    3,
                    0));
        }
        return jPanel11;
    }
    String currentUser = null;
    class AppActionListener implements ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent event) {
            String action = event.getActionCommand();
            try {
                String parm1, parm2, result = null;
                if (action.equalsIgnoreCase("Login")) {
                    parm1 = jTextField.getText(); // userID
                    parm2 = new String(jPasswordField1.getPassword()); // password
                    AccountDataBean accountData =
                        getTrade().login(parm1, parm2);
                    result = accountData.toString();
                    currentUser = accountData.getProfileID();
                } else if (action.equalsIgnoreCase("MarketSummary")) {
                    MarketSummaryDataBeanWS marketSummary =
                        getTrade().getMarketSummary();
                    result = marketSummary.toString();
                } else  if (action.equalsIgnoreCase("Quotes")) {
                    parm1 = jTextField2.getText(); // symbol
                    QuoteDataBean quoteData = getTrade().getQuote(parm1);
                    result = quoteData.toString();

                }

                else if (currentUser==null)
                {
                    result = "Error: User login is required for this operation";
                }
                else if (action.equalsIgnoreCase("Account")) {
                    AccountDataBean accountData =
                        getTrade().getAccountData(currentUser);
                    AccountProfileDataBean accountProfileData =
                        getTrade().getAccountProfileData(currentUser);
                    result =
                        accountData.toString() + accountProfileData.toString();
                } else if (action.equalsIgnoreCase("Buy")) {
                    parm1 = jTextField3.getText(); // symbol
                    parm2 = jTextField4.getText(); // quantity
                    OrderDataBean orderData =
                        getTrade().buy(
                            currentUser,
                            parm1,
                            Double.parseDouble(parm2),
                            0);
                    result = orderData.toString();
                } else if (action.equalsIgnoreCase("Sell")) {
                    parm1 = jTextField5.getText(); // holding ID
                    OrderDataBean orderData =
                        getTrade().sell(
                            currentUser,
                            Integer.valueOf(parm1),
                            0);
                    result = orderData.toString();
                } else if (action.equalsIgnoreCase("Orders")) {
                    Object[] orders = getTrade().getOrders(currentUser);
                    if (orders.length == 0) {
                        result = "No orders";
                    }
                    else {
                        result = "";
                        for (int i = 0; i < orders.length; i++)
                            result += new StringBuffer().append("\n").append(orders[i].toString()).toString();
                    }

                } else if (action.equalsIgnoreCase("Holdings")) {
                    Object[] holdings = getTrade().getHoldings(currentUser);
                    if (holdings.length == 0) {
                        result = "No holdings";
                    }
                    else {
                        result = "";
                        for (int i = 0; i < holdings.length; i++)
                            result += "\n"
                                + holdings[i].toString();
                    }

                } else if (action.equalsIgnoreCase("Logout")) {
                    getTrade().logout(currentUser);
                    result = "\n\n\tUser: " + currentUser + " logged out";
                    currentUser = null;
                }
                jTextArea.setText(result);
            } catch (Exception e1) {
                System.out.println("ClientApp error;\n" + e1.toString());
                e1.printStackTrace();
            }
        }
    }

    private void startScenario(int numThreads, int reqPerThread) {
        output.setText(
            "Scenario: Starting "
                + numThreads
                + " threads running "
                + reqPerThread
                + " requests each");

        //TODO - code generic scenario
        threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new ClientScenario(reqPerThread);
            threads[i].setName("ScenarioThreads " + i);
        }
        ClientScenario.setNumThreads(numThreads);
        ClientScenario.setStartTime(0);
        ClientScenario.setServicePort(getJTextField12().getText());
        //TODO this is a cheap scenario implementation -- needs to be generic
        String scenario = (String) jComboBox.getSelectedItem();
        if ( scenario.equalsIgnoreCase("Get Quote - NULL")	)
        {
            ClientScenario.symbol = null;
        }
        else
        {
            ClientScenario.symbol = null;
        }
        System.out.println("Running Scenario: " + scenario + " symbol = " + ClientScenario.symbol);
        //Start a timer daemon to gather statistics
        stats = new java.util.Timer(true);
        stats.scheduleAtFixedRate(new StatsTimerTask(), 1000, 5000);
        for (int i = 0; i < numThreads; i++)
            threads[i].start();
    }

    private void stopScenario() {

        // Signal all scenario threads to stop
        output.setText("Stopping all scenario threads");
        long numThreads = ClientScenario.getNumThreads();
        for (int i = 0; i < numThreads; i++) {
            ClientScenario client = (ClientScenario) threads[i];
            client.setStop(true);
        }

        stats.cancel();
        output.append(
            "\nstopScenario:  All scenario threads and statkeeper stopped");
    }

    private void clearScenarioStats() {

        //TODO min Resp is not working after clear - know why avg = 0.0 after clear
        int currTotReq = 0;
        long numThreads = ClientScenario.getNumThreads();
        for (int i = 0; i < numThreads; i++) {
            ClientScenario client = (ClientScenario) threads[i];
            currTotReq += client.clearStats();
        }
        ClientScenario.setTotReqsAtLastInterval(currTotReq);
        ClientScenario.setStatStartTime(System.currentTimeMillis());
        jLabel14.setText("0");
        jLabel15.setText("0");

        jLabel22.setText("0.0");
        jLabel23.setText("0.0");
        jLabel24.setText("0.0");
    }

    Thread[] threads;
    java.util.Timer stats;
    JTextArea output;

    class StatsTimerTask extends TimerTask {
        public void run() {
            long totReqs = 0, totErrs = 0, statReqs = 0, totResp = 0;
            long totTime = 0, currTime;

            // Scenario just started?
            if (ClientScenario.getStartTime() == 0) {
                //Clear stats
                clearScenarioStats();
                ClientScenario.setStartTime(System.currentTimeMillis());
                return;
            }

            // First get the rawdata from each thread
            for (int i = 0; i < threads.length; i++) {
                ClientScenario thread = ((ClientScenario) threads[i]);
                totReqs += thread.getNumReqs();
                totErrs += thread.getNumErrs();
                totResp += thread.getTotResp();
                statReqs += thread.getNumStatReqs();
            }
            float avgResp = (float)totResp / (float)statReqs;
            long maxResp = ClientScenario.getMaxResp();
            long minResp = ClientScenario.getMinResp();

            currTime = System.currentTimeMillis();
            long totalTime 	= currTime - ClientScenario.getStartTime();
            long intervalTime 	= currTime - ClientScenario.getIntervalStartTime();
            long statTime 		= currTime - ClientScenario.getStatStartTime();
            ClientScenario.setIntervalStartTime(currTime);

            // Scenario Finished?
            if ((totReqs + totErrs) >= ClientScenario.getTotalNumRequests()) {
                output.setText(
                    "Scenario: Run completed:"
                        + totReqs
                        + " requests in"
                        + totTime / 1000
                        + " seconds");
                this.cancel();
                jLabel16.setText("" + totReqs);
                jLabel17.setText("" + totErrs);
                jLabel25.setText("" + totTime / 1000);
                jButton9.setText("Start Scenario!");
                jButton9.setActionCommand("start");
                jProgressBar.setValue(100);
                return;
            }
            BigDecimal	reqPerSec =
                    new BigDecimal(
                        (double) statReqs / (double)statTime * 1000.0).setScale(
                        2,
                        ROUND);
            BigDecimal 	lastReqPerSec =
                    new BigDecimal(
                        (double) (totReqs - ClientScenario.getTotReqsAtLastInterval())
                            / (double) intervalTime
                            * 1000).setScale(
                        2,
                        ROUND);
            BigDecimal	avgRespTime =
                    new BigDecimal(avgResp).setScale(2, ROUND);
            BigDecimal	maxRespBD = new BigDecimal(maxResp).setScale(2, ROUND);
            BigDecimal	minRespBD = new BigDecimal(minResp).setScale(2, ROUND);
            int 		percentComplete =
                    (int) (100.0
                        * totReqs
                        / (double) (ClientScenario.getTotalNumRequests()));

            jLabel14.setText(reqPerSec.toString());
            jLabel15.setText(lastReqPerSec.toString());
            jLabel16.setText("" + totReqs);
            jLabel17.setText("" + totErrs);

            jLabel22.setText(avgRespTime.toString());
            jLabel23.setText(minRespBD.toString());
            jLabel24.setText(maxRespBD.toString());
            jLabel25.setText("" + totTime / 1000);

            jProgressBar.setValue(percentComplete);
            ClientScenario.setTotReqsAtLastInterval(totReqs);
        }
    }

    private static TradeWSServices trade = null;
    TradeWSServices getTrade() throws Exception {
        ClientScenario.setServicePort(getJTextField12().getText());
        return ClientScenario.getTradeSingleton();
    }

    private static ClientScenario client = null;
    ClientScenario getClientScenario() {
        if (client == null)
            client = new ClientScenario();
        return client;
    }
    //miscellaneous
    private int ROUND = BigDecimal.ROUND_HALF_UP;
    private static final BigDecimal ZERO = new BigDecimal(0.0);


     private JMenuItem jMenuItem2 = null;
     private JLabel jLabel27 = null;
     private JTextField jTextField12 = null;
    /**
     * This method initializes jMenuItem2
     *
     * @return JMenuItem
     */
    private JMenuItem getJMenuItem2() {
        if(jMenuItem2 == null) {
            jMenuItem2 = new JMenuItem();
            jMenuItem2.setText("Service Port URL");
            jMenuItem2.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String command = e.getActionCommand();
                    if (command.equalsIgnoreCase("start")) {
                        int numThreads = Integer.parseInt(jTextField7.getText());
                        int reqPerThread =
                            Integer.parseInt(jTextField6.getText())
                                / numThreads;
                        startScenario(numThreads, reqPerThread);
                        jButton9.setText("Stop");
                        jButton9.setActionCommand("stop");
                    } else if (command.equalsIgnoreCase("stop")) {
                        stopScenario();
                        jButton9.setText("Start Scenario!");
                        jButton9.setActionCommand("start");

                    }
                }
            }
            );
        }
        return jMenuItem2;
    }
    /**
     * This method initializes jLabel27
     *
     * @return JLabel
     */
    private JLabel getJLabel27() {
        if(jLabel27 == null) {
            jLabel27 = new JLabel();
            jLabel27.setBounds(9, 17, 122, 14);
            jLabel27.setText("Service Port URL");
            jLabel27.setToolTipText("URL pointing to the concrete WSDL for the service");
            jLabel27.setFont(new java.awt.Font("sansserif", 1, 10));
        }
        return jLabel27;
    }
    /**
     * This method initializes jTextField12
     *
     * @return JTextField
     */
    private JTextField getJTextField12() {
        if(jTextField12 == null) {
            jTextField12 = new JTextField();
            jTextField12.setBounds(138, 16, 202, 16);
            jTextField12.setFont(new java.awt.Font("sansserif", 1, 10));
            jTextField12.setText(ClientScenario.getServicePort());
        }
        return jTextField12;
    }
} //  @jve:visual-info  decl-index=0 visual-constraint="0,0"
