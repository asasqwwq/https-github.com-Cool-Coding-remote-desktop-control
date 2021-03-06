package cn.yang.master.client.ui.impl;

import cn.yang.common.command.Commands;
import cn.yang.common.util.BeanUtil;
import cn.yang.common.util.TaskExecutors;
import cn.yang.master.client.constant.ExceptionMessageConstants;
import cn.yang.master.client.constant.MessageConstants;
import cn.yang.master.client.exception.MasterClientException;
import cn.yang.master.client.netty.MasterNettyClient;
import cn.yang.master.client.ui.IDisplayPuppet;
import cn.yang.master.client.ui.IMasterDesktop;
import org.springframework.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Cool-Coding
 *         2018/7/26
 */
public class MasterDesktop extends JFrame implements IMasterDesktop,ActionListener{
    private MasterNettyClient masterClient;
    private HashMap<String, IDisplayPuppet> puppets=new HashMap<>();

    private JTextField puppetNameTextField;

    public MasterDesktop(){
      setting();
      initMenu();
      initBody();
    }

    @Override
    public void lanuch(){
        try {
            SwingUtilities.invokeAndWait(() -> {
                setVisible(true);
            });
            connect();
        }catch (Exception e){
            popToShowMessage(ExceptionMessageConstants.LAUNCH_FAILED,e.getMessage());
        }
    }

    @Override
    public void setting(){
        setBackground(new Color(255,255,255));
        setSize(450,300);
        setResizable(false);
        setLocation(250, 250);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                TaskExecutors.shutdown();
                for(String puppetName:puppets.keySet()){
                    terminate0(puppetName);
                }
            }
        });
    }

    @Override
    public void initMenu(){
        JMenuBar menuBar=new JMenuBar();
        setJMenuBar(menuBar);

        JMenu jMenu=new JMenu("??????");
        menuBar.add(jMenu);

        JMenuItem connect=new JMenuItem("??????");
        connect.setActionCommand(Commands.CONNECT.name());
        connect.addActionListener(this);
        jMenu.add(connect);

        JMenuItem control=new JMenuItem("??????");
        control.setActionCommand(Commands.CONTROL.name());
        control.addActionListener(this);
        jMenu.add(control);
    }

    @Override
    public void initBody(){
        Font titleFont=new Font("??????",Font.BOLD,25);
        Font contentFont=new Font("??????",Font.PLAIN,20);

        JPanel jPanel=new JPanel(new GridLayout(3,1));

        JPanel titlePanel=new JPanel();
        JTextArea jTitle=new JTextArea(3,10);
        jTitle.setText("??????????????????");
        jTitle.setForeground(Color.decode("#36648B"));
        jTitle.setFont(titleFont);
        jTitle.setOpaque(false);
        jTitle.setEditable(false);
        titlePanel.add(jTitle);
        jPanel.add(titlePanel);

        JPanel puppetNamePanel=new JPanel();
        jPanel.add(puppetNamePanel);
        JLabel jLabel=new JLabel();
        jLabel.setText("???????????????:");
        jLabel.setFont(contentFont);
        puppetNamePanel.add(jLabel);

        puppetNameTextField =new JTextField(30);
        puppetNameTextField.setToolTipText("?????????????????????");
        puppetNameTextField.setFont(contentFont);
        puppetNamePanel.add(puppetNameTextField);

        JPanel remoteButtonPanel=new JPanel();
        jPanel.add(remoteButtonPanel);
        JButton jButton=new JButton();
        jButton.setText("??????");
        jButton.setActionCommand(Commands.CONTROL.name());
        jButton.addActionListener(this);
        remoteButtonPanel.add(jButton);

        final Container contentPane = getContentPane();
        contentPane.add(BorderLayout.CENTER,jPanel);
    }

    /**
     * ???????????????????????????
     * @param puppetName
     */
    @Override
    public void lanuch(String puppetName){
        final IDisplayPuppet puppetScreen = BeanUtil.getBean(IDisplayPuppet.class,puppetName);
        puppets.put(puppetName,puppetScreen);
        puppetScreen.launch();
    }

    @Override
    public void refreshScreen(String puppetName,byte[] bytes) {
        //??????????????????????????????????????????????????????????????????????????????????????????
        // (???????????????????????????????????????????????????????????????????????????????????????)
        final IDisplayPuppet puppetScreen = puppets.get(puppetName);
        if (puppetScreen != null) {
            puppetScreen.refresh(bytes);
        }
    }

    @Override
    public void connect() throws Exception{
        masterClient.connect();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
       switch (Commands.valueOf(e.getActionCommand())){
           case CONNECT:
               try {
                   connect();
                   popToShowMessage(MessageConstants.CONNECT_SUCCESSFULLY);
               }catch (Exception e2){
                   popToShowMessage(ExceptionMessageConstants.CONNECTION_SERVER_FAILED,e2.getMessage());
               }
               break;
           case CONTROL:
               if (StringUtils.isEmpty(puppetNameTextField.getText())){
                   popToShowMessage(ExceptionMessageConstants.PUPPET_NAME_EMPTY);
               }else {
                   String puppetName=puppetNameTextField.getText();
                   try {
                       masterClient.fireCommand(puppetName, Commands.CONTROL, null);
                   }catch (MasterClientException e2){
                        popToShowMessage(e2.getMessage());
                   }
               }
            break;
            default:
       }
    }

    @Override
    public void terminate(String puppetName){
        terminate0(puppetName);
        puppets.remove(puppetName);
    }

    private void terminate0(String puppetName){
        try {
            masterClient.fireCommand(puppetName, Commands.TERMINATE, null);
        }catch (MasterClientException e){
            popToShowMessage(e.getMessage());
        }
    }

    @Override
    public void fireCommand(String puppetName, Enum<Commands> command, Object data) throws MasterClientException {
        masterClient.fireCommand(puppetName,command,data);
    }

    public void setMasterClient(MasterNettyClient client) {
        this.masterClient = client;
    }

    public void showMessage(String message){
        JOptionPane.showMessageDialog(this,message);
    }

    private void popToShowMessage(String... message){
        JOptionPane.showMessageDialog(this, Arrays.toString(message));
    }
}
