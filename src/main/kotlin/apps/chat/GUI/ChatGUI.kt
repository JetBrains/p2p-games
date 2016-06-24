package apps.chat.GUI

/**
 * Created by user on 6/22/16.
 */

import apps.chat.Chat
import java.awt.*
import java.awt.event.*
import javax.swing.*

/**
 * Simple chat gui. Almost entirely found on internet
 */
class ChatGUI(internal var chat: Chat) {

    internal var appName: String = "P2P chat window(loading)"
    internal var chatFrame = JFrame(appName)
    internal var sendMessage: JButton = JButton()
    internal var messageBox: JTextField = JTextField()
    internal var chatBox: JTextArea = JTextArea()
    var isClosed: Boolean = false

    //Todo - remove callbacks

    fun display() {
        val mainPanel = JPanel()
        mainPanel.layout = BorderLayout()

        val southPanel = JPanel()
        southPanel.background = Color.BLUE
        southPanel.layout = GridBagLayout()
        val listener = sendMessageButtonListener()

        messageBox = JTextField(30)
        messageBox.requestFocusInWindow()
        messageBox.addActionListener(listener)

        sendMessage = JButton("Send Message")
        sendMessage.addActionListener(listener)

        chatBox = JTextArea()
        chatBox.isEditable = false
        chatBox.font = Font("Serif", Font.PLAIN, 15)
        chatBox.lineWrap = true

        mainPanel.add(JScrollPane(chatBox), BorderLayout.CENTER)

        val left = GridBagConstraints()
        left.anchor = GridBagConstraints.LINE_START
        left.fill = GridBagConstraints.HORIZONTAL
        left.weightx = 512.0
        left.weighty = 1.0

        val right = GridBagConstraints()
        right.insets = Insets(0, 10, 0, 0)
        right.anchor = GridBagConstraints.LINE_END
        right.fill = GridBagConstraints.NONE
        right.weightx = 1.0
        right.weighty = 1.0

        southPanel.add(messageBox, left)
        southPanel.add(sendMessage, right)

        mainPanel.add(BorderLayout.SOUTH, southPanel)

        chatFrame.add(mainPanel)
        chatFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        chatFrame.setSize(470, 300)
        chatFrame.isVisible = true
        chatFrame.addWindowListener(object: WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                isClosed = true
            }
        })
    }

    fun reopen(){
        isClosed = false
        chatFrame.isVisible = true
    }

    fun refreshTitle(title: String){
        appName = title
        chatFrame.title = title
    }
    fun displayMessage(user: String, msg: String) {
        chatBox.append("<$user>:  $msg\n")
    }


    internal inner class sendMessageButtonListener : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            if (messageBox.text.length < 1) {
                // do nothing
            } else if (messageBox.text.equals(".clear")) {
                chatBox.text = "Cleared all messages\n"
                messageBox.text = ""
            } else {
                chat.sendMessage(messageBox.text)
                messageBox.text = ""
            }
            messageBox.requestFocusInWindow()
        }
    }




}