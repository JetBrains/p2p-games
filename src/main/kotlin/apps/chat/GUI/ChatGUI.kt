package apps.chat.GUI

/**
 * Created by user on 6/22/16.
 */

import apps.chat.Chat
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

/**
 * Simple chat gui. Almost entirely found on internet
 */
class ChatGUI() {

    internal var appName = "P2Pchat"
    internal var chatGUI: ChatGUI? = null
    internal var newFrame = JFrame(appName)
    internal var sendMessage: JButton = JButton()
    internal var messageBox: JTextField = JTextField()
    internal var chatBox: JTextArea = JTextArea()
    internal var usernameChooser: JTextField = JTextField()
    internal var preFrame: JFrame = JFrame()

    //Todo - remove callbacks
    internal var chat: Chat? = null

    fun preDisplay() {
        newFrame.isVisible = false
        preFrame = JFrame(appName)
        usernameChooser = JTextField(15)
        val chooseUsernameLabel = JLabel("Pick a username:")
        val enterServer = JButton("Enter Chat Server")
        enterServer.addActionListener(enterServerButtonListener())
        val prePanel = JPanel(GridBagLayout())

        val preRight = GridBagConstraints()
        preRight.insets = Insets(0, 0, 0, 10)
        preRight.anchor = GridBagConstraints.EAST
        val preLeft = GridBagConstraints()
        preLeft.anchor = GridBagConstraints.WEST
        preLeft.insets = Insets(0, 10, 0, 10)
        // preRight.weightx = 2.0;
        preRight.fill = GridBagConstraints.HORIZONTAL
        preRight.gridwidth = GridBagConstraints.REMAINDER

        prePanel.add(chooseUsernameLabel, preLeft)
        prePanel.add(usernameChooser, preRight)
        preFrame.add(BorderLayout.CENTER, prePanel)
        preFrame.add(BorderLayout.SOUTH, enterServer)
        preFrame.setSize(600, 400)
        preFrame.isVisible = true

    }

    fun display() {
        val mainPanel = JPanel()
        mainPanel.layout = BorderLayout()

        val southPanel = JPanel()
        southPanel.background = Color.BLUE
        southPanel.layout = GridBagLayout()

        messageBox = JTextField(30)
        messageBox.requestFocusInWindow()

        sendMessage = JButton("Send Message")
        sendMessage.addActionListener(sendMessageButtonListener())

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

        newFrame.add(mainPanel)
        newFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        newFrame.setSize(470, 300)
        newFrame.isVisible = true
    }

    fun displayMessage(chat: Int, user: String, msg: String) {
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
                //displayMessage(chat!!.chatId, username, messageBox.text)
                chat!!.sendMessage(messageBox.text)
                messageBox.text = ""
            }
            messageBox.requestFocusInWindow()
        }
    }

    internal var username: String = ""

    internal inner class enterServerButtonListener : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            username = usernameChooser.text
            //todo better register
            chat!!.register(username)
            if (username.length < 1) {
                println("No!")
            } else {
                preFrame.isVisible = false
                display()
            }
        }

    }

}