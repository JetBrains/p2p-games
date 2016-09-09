package apps.chat.gui

import Settings
import apps.chat.ChatManager
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.text.NumberFormat
import javax.swing.*

/**
 * Created by user on 6/24/16.
 */
class ChatManagerGUI() {
    internal val appName = "P2P chat + [${Settings.hostAddress.port}]"
    internal var usernameChooser: JTextField = JTextField()
    internal var hostAddressChooser: JTextField = JTextField()
    internal var chatIDChooser: JFormattedTextField = JFormattedTextField()
    internal var enterFrame: JFrame = JFrame()

    fun display() {
        enterFrame = JFrame(appName)
        usernameChooser = JTextField(Settings.defaultUsername, 20)
        //hostAddressChooser = JTextField("${Settings.hostAddress.hostName}:${Settings.hostAddress.port}", 20)
        hostAddressChooser = JTextField("${Settings.hostAddress.hostName}:1232",
                20)
        chatIDChooser = JFormattedTextField(NumberFormat.getNumberInstance())
        chatIDChooser.text = "666"
        chatIDChooser.columns = 4
        val chooseUsernameLabel = JLabel("Pick a username:")
        val chooseHostLabel = JLabel("Specify a peer:")
        val chooseChatLabel = JLabel("Enter chat ID:")
        val enterServer = JButton("Connect")
        enterServer.addActionListener(enterServerButtonListener())
        val chatEnterPanel = JPanel(GridBagLayout())

        val usernameRight = GridBagConstraints()
        usernameRight.insets = Insets(0, 0, 0, 10)
        usernameRight.anchor = GridBagConstraints.WEST
        val usernameLeft = GridBagConstraints()
        usernameLeft.anchor = GridBagConstraints.EAST
        usernameLeft.insets = Insets(0, 10, 0, 10)
        // usernameRight.weightx = 2.0;
        usernameRight.fill = GridBagConstraints.HORIZONTAL
        usernameRight.gridwidth = GridBagConstraints.REMAINDER

        chatEnterPanel.add(chooseUsernameLabel, usernameLeft)
        chatEnterPanel.add(usernameChooser, usernameRight)


        val hostRight = GridBagConstraints()
        hostRight.insets = Insets(30, 0, 30, 10)
        hostRight.anchor = GridBagConstraints.WEST
        val hostLeft = GridBagConstraints()
        hostLeft.anchor = GridBagConstraints.EAST
        hostLeft.insets = Insets(30, 10, 30, 10)
        // usernameRight.weightx = 2.0;
        hostRight.fill = GridBagConstraints.HORIZONTAL
        hostRight.gridwidth = GridBagConstraints.REMAINDER


        chatEnterPanel.add(chooseHostLabel, hostLeft)
        chatEnterPanel.add(hostAddressChooser, hostRight)

        val chatRight = GridBagConstraints()
        chatRight.insets = Insets(20, 0, 20, 10)
        chatRight.anchor = GridBagConstraints.WEST
        val chatLeft = GridBagConstraints()
        chatLeft.anchor = GridBagConstraints.EAST
        chatLeft.insets = Insets(20, 10, 20, 10)
        // usernameRight.weightx = 2.0;
        chatRight.fill = GridBagConstraints.NONE
        chatRight.gridwidth = GridBagConstraints.REMAINDER

        chatEnterPanel.add(chooseChatLabel, chatLeft)
        chatEnterPanel.add(chatIDChooser, chatRight)


        enterFrame.add(BorderLayout.CENTER, chatEnterPanel)
        enterFrame.add(BorderLayout.SOUTH, enterServer)
        enterFrame.setSize(600, 400)
        enterFrame.isVisible = true
        enterFrame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        enterFrame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                close()
            }
        })

    }

    internal inner class enterServerButtonListener : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            if (usernameChooser.text.isEmpty()) {
                return
            }
            try {
                val uri = URI("my://" + hostAddressChooser.text)
                if (uri.port == -1) {
                    return
                }
                val hostAddr = InetSocketAddress(uri.host, uri.port)
                chatIDChooser.commitEdit()
                val chatId = (chatIDChooser.value as Number).toInt()
                ChatManager.joinChat(chatId, hostAddr, usernameChooser.text)
            } catch(ignored: URISyntaxException) {
                System.err.println("Malformed host address")
            }

        }

    }

    internal fun close() {
        ChatManager.close()
    }
}