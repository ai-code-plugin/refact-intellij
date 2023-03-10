package com.obiscr.chatgpt.ui

import com.intellij.openapi.ui.NullableComponent
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.smallcloud.codify.account.AccountManager
import com.smallcloud.codify.panes.gptchat.State
import com.smallcloud.codify.panes.gptchat.ui.MessageComponent
import com.smallcloud.codify.panes.gptchat.utils.md2html
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.JScrollBar
import javax.swing.ScrollPaneConstants


class MyScrollPane(view: Component?, vsbPolicy: Int, hsbPolicy: Int) : JBScrollPane(view, vsbPolicy, hsbPolicy) {
    override fun updateUI() {
        border = null
        super.updateUI()
    }

    override fun setCorner(key: String?, corner: Component?) {
        border = null
        super.setCorner(key, corner)
    }
}

class ShiftedMessage(val message: MessageComponent): JPanel() {
    val shift = Box.createRigidArea(Dimension(30, 0))
    init {
        layout = VerticalLayout()
        if (message.me) {
//            layout = FlowLayout(FlowLayout.RIGHT)
            add(shift/*, BorderLayout.WEST*/)
//            add
            add(message/*, BorderLayout.EAST*/)
        } else {
            add(message/*, BorderLayout.WEST*/)
//            layout = FlowLayout(FlowLayout.LEFT)
            add(shift/*, BorderLayout.EAST*/)
        }
    }
}



class HistoryComponent: JBPanel<HistoryComponent>(), NullableComponent {
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private val myScrollPane: MyScrollPane = MyScrollPane(myList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    private var myScrollValue = 0
    private val scrollListener = MyAdjustmentListener()
    private val tip: MessageComponent
    init {
        border = JBUI.Borders.empty(10, 10, 10, 0)
        layout = BorderLayout(JBUI.scale(7), 0)
        background = UIUtil.getListBackground()
        val mainPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
        mainPanel.isOpaque = false
        mainPanel.border = JBUI.Borders.emptyLeft(8)
        add(mainPanel)
        val myTitle = JBLabel("Conversation")
        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.border = JBUI.Borders.empty(0, 10, 10, 0)
        panel.add(myTitle, BorderLayout.WEST)
        val newChat = LinkLabel<String>("New chat", null)
        var helloString = "### Hi! \uD83D\uDC4B\n" +
                "### This chat has more features and it's more responsive than a free one you might find on the web."
        if (!AccountManager.isLoggedIn) {
            helloString += " Don't forget to log in!"
        }
        tip = MessageComponent(md2html(helloString), false)

        newChat.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                clearHistory()
                add(tip)
            }
        })
        newChat.font = JBFont.label()
        newChat.border = JBUI.Borders.emptyRight(20)
        panel.add(newChat, BorderLayout.EAST)
        mainPanel.add(panel, BorderLayout.NORTH)
        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()
        myList.border = JBUI.Borders.emptyRight(10)
        myScrollPane.setBorder(null)
        mainPanel.add(myScrollPane)
        myScrollPane.getVerticalScrollBar().setAutoscrolls(true)
        myScrollPane.getVerticalScrollBar().addAdjustmentListener { e ->
            val value: Int = e.getValue()
            if (myScrollValue == 0 && value > 0 || myScrollValue > 0 && value == 0) {
                myScrollValue = value
                repaint()
            } else {
                myScrollValue = value
            }
        }
        add(tip)
    }

    fun clearHistory() {
        State.instance.clear()
        myList.removeAll()
    }

    fun add(messageComponent: MessageComponent) {
        if (myList.componentCount > 0 && (myList.getComponent(0) as ShiftedMessage).message == tip) {
            myList.remove(0)
        }
        myList.add(ShiftedMessage(messageComponent))
        updateLayout()
        scrollToBottom()
        updateUI()
    }
    fun lastMessage(): MessageComponent {
        return (myList.getComponent(myList.componentCount - 1) as ShiftedMessage).message
    }


    fun scrollToBottom() {
        val verticalScrollBar: JScrollBar = myScrollPane.getVerticalScrollBar()
        verticalScrollBar.value = verticalScrollBar.maximum
    }

    fun updateLayout() {
        val layout = myList.layout
        val componentCount = myList.componentCount
        for (i in 0 until componentCount) {
            layout.removeLayoutComponent(myList.getComponent(i))
            layout.addLayoutComponent(null, myList.getComponent(i))
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (myScrollValue > 0) {
            g.color = JBColor.border()
            val y: Int = myScrollPane.getY() - 1
            g.drawLine(0, y, width, y)
        }
    }

    override fun isVisible(): Boolean {
        if (super.isVisible()) {
            val count = myList.componentCount
            for (i in 0 until count) {
                if (myList.getComponent(i).isVisible) {
                    return true
                }
            }
        }
        return false
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    internal class MyAdjustmentListener : AdjustmentListener {
        override fun adjustmentValueChanged(e: AdjustmentEvent) {
            val source = e.source as JScrollBar
            if (!source.valueIsAdjusting) {
                source.value = source.maximum
            }
        }
    }

    fun addScrollListener() {
        myScrollPane.getVerticalScrollBar().addAdjustmentListener(scrollListener)
    }

    fun removeScrollListener() {
        myScrollPane.getVerticalScrollBar().removeAdjustmentListener(scrollListener)
    }
}