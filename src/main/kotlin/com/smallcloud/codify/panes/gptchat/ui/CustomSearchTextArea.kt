package com.smallcloud.codify.panes.gptchat.ui

import com.intellij.find.FindBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.EditorCopyPasteHelper
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.LightEditActionFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StartupUiUtil
import com.intellij.util.ui.UIUtil
import com.smallcloud.codify.CodifyBundle
import java.awt.*
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit.InsertBreakAction
import javax.swing.text.PlainDocument


class CustomSearchTextArea(val textArea: JTextArea) : JPanel(), PropertyChangeListener {
    private val myIconsPanel: JPanel = NonOpaquePanel()
    private val myNewLineButton: ActionButton
    private val myClearButton: ActionButton
    private val myAddSelectedLinesCB: JCheckBox
    private val myExtraActionsPanel = NonOpaquePanel()
    private val myScrollPane: JBScrollPane
    private var myMultilineEnabled = true

    override fun updateUI() {
        super.updateUI()
        updateFont()
        background = BACKGROUND_COLOR
    }

    private fun updateFont() {
        if (textArea != null) {
            if (Registry.`is`("ide.find.use.editor.font", false)) {
                textArea.font = EditorUtil.getEditorFont()
            } else {
                textArea.font = UIManager.getFont("TextField.font")
            }
        }
    }

    protected fun updateLayout() {
        val iconsPanelWrapper: JPanel = NonOpaquePanel(BorderLayout())
        iconsPanelWrapper.border = JBUI.Borders.emptyTop(2)
        val p: JPanel = NonOpaquePanel(BorderLayout())
        p.add(myIconsPanel, BorderLayout.NORTH)
        myIconsPanel.border = JBUI.Borders.emptyRight(5)
        iconsPanelWrapper.add(p, BorderLayout.WEST)
        iconsPanelWrapper.add(myExtraActionsPanel, BorderLayout.SOUTH)
        removeAll()
        border = JBUI.Borders.empty(JBUI.insets("Editor.SearchField.borderInsets", JBUI.insets(if (SystemInfo.isLinux) 2 else 1)))
        layout = BorderLayout()
        add(NonOpaquePanel().apply {
            layout = GridBagLayout()
            val gc = GridBagConstraints()
            gc.gridx = 0
            gc.gridy = 0
            gc.anchor = GridBagConstraints.NORTHWEST
            gc.fill = GridBagConstraints.BOTH
            gc.weightx = 2.0
            gc.weighty = 2.0
            add(myScrollPane, gc)

            gc.gridx = 0
            gc.gridy = 1
            gc.anchor = GridBagConstraints.SOUTHWEST
            gc.fill = GridBagConstraints.NONE
            add(myAddSelectedLinesCB, gc)

            gc.gridx = 1
            gc.gridy = 0
            gc.gridheight = 2
            gc.anchor = GridBagConstraints.EAST
            gc.weightx = 0.0
            gc.weighty = 2.0
            gc.fill = GridBagConstraints.VERTICAL
            add(iconsPanelWrapper, gc)
        }, BorderLayout.CENTER)
        updateIconsLayout()
    }

    private fun updateIconsLayout() {
        if (myIconsPanel.parent == null) {
            return
        }
        val showClearIcon = !StringUtil.isEmpty(textArea.text)
        val showNewLine = myMultilineEnabled
        val wrongVisibility = myClearButton.parent == null == showClearIcon || myNewLineButton.parent == null == showNewLine
        val multiline = StringUtil.getLineBreakCount(textArea.text) > 0
        if (wrongVisibility) {
            myIconsPanel.removeAll()
            myIconsPanel.layout = BorderLayout()
            myIconsPanel.add(myClearButton, BorderLayout.CENTER)
            myIconsPanel.add(myNewLineButton, BorderLayout.EAST)
            myIconsPanel.preferredSize = myIconsPanel.preferredSize
            if (!showClearIcon) myIconsPanel.remove(myClearButton)
            if (!showNewLine) myIconsPanel.remove(myNewLineButton)
            myIconsPanel.revalidate()
            myIconsPanel.repaint()
        }
        myScrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        myScrollPane.verticalScrollBarPolicy = if (multiline) ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED else ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
        myScrollPane.horizontalScrollBar.isVisible = multiline
        myScrollPane.revalidate()
        doLayout()
    }

    fun setExtraActions(vararg actions: AnAction): List<Component> {
        myExtraActionsPanel.removeAll()
        myExtraActionsPanel.border = JBUI.Borders.empty()
        val addedButtons = ArrayList<Component>()
        if (actions != null && actions.size > 0) {
            val buttonsGrid: JPanel = NonOpaquePanel(GridLayout(1, actions.size, JBUI.scale(4), 0))
            for (action in actions) {
                if (action is TooltipDescriptionProvider) {
                    action.templatePresentation.description = FindBundle.message("find.embedded.buttons.description")
                }
                val button: ActionButton = MyActionButton(action, true, true)
                addedButtons.add(button)
                buttonsGrid.add(button)
            }
            buttonsGrid.border = JBUI.Borders.emptyRight(2)
            myExtraActionsPanel.layout = BorderLayout()
            myExtraActionsPanel.add(buttonsGrid, BorderLayout.NORTH)
        }
        return addedButtons
    }

    fun updateExtraActions() {
        for (button in UIUtil.findComponentsOfType(myExtraActionsPanel, ActionButton::class.java)) {
            button.update()
        }
    }

    private val myEnterRedispatcher: KeyAdapter = object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyCode == KeyEvent.VK_ENTER && parent != null) {
                parent.dispatchEvent(e)
            }
        }
    }

    init {
        updateFont()
        textArea.addPropertyChangeListener("background", this)
        textArea.addPropertyChangeListener("font", this)
        LightEditActionFactory.create { event: AnActionEvent? -> textArea.transferFocus() }
                .registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), textArea)
        LightEditActionFactory.create { event: AnActionEvent? -> textArea.transferFocusBackward() }
                .registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)), textArea)
        KeymapUtil.reassignAction(textArea, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), NEW_LINE_KEYSTROKE, WHEN_FOCUSED)
        textArea.document = object : PlainDocument() {
            @Throws(BadLocationException::class)
            override fun insertString(offs: Int, str: String, a: AttributeSet?) {
                var str = str
                if (getProperty("filterNewlines") === java.lang.Boolean.TRUE && str.indexOf('\n') >= 0) {
                    str = StringUtil.replace(str, "\n", " ")
                }
                if (!StringUtil.isEmpty(str)) super.insertString(offs, str, a)
            }
        }
        if (Registry.`is`("ide.find.field.trims.pasted.text", false)) {
            textArea.document.putProperty(EditorCopyPasteHelper.TRIM_TEXT_ON_PASTE_KEY, java.lang.Boolean.TRUE)
        }
        textArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                if (e.type == DocumentEvent.EventType.INSERT) {
                    textArea.putClientProperty(JUST_CLEARED_KEY, null)
                }
                val rows = Math.min(Registry.get("ide.find.max.rows").asInteger(), textArea.lineCount)
                textArea.rows = Math.max(1, Math.min(25, rows))
                updateIconsLayout()
            }
        })
        textArea.isOpaque = false
        myScrollPane = object : JBScrollPane(textArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            override fun setupCorners() {
                super.setupCorners()
                super.setBorder(EMPTY_SCROLL_BORDER)
            }

            override fun updateUI() {
                super.updateUI()
                super.setBorder(EMPTY_SCROLL_BORDER)
            }

            // Disable external updates e.g. from UIUtil.removeScrollBorder
            override fun setBorder(border: Border) {}
        }
        textArea.border = object : Border {
            override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {}
            override fun getBorderInsets(c: Component): Insets {
                return if (SystemInfo.isMac) {
                    JBInsets(3, 0, 2, 0)
                } else {
                    var bottom = if (StringUtil.getLineBreakCount(textArea.text) > 0) 2 else if (StartupUiUtil.isUnderDarcula()) 1 else 0
                    var top = if (textArea.getFontMetrics(textArea.font).height <= 16) 2 else 1
                    if (JBUIScale.isUsrHiDPI()) {
                        bottom = 0
                        top = 2
                    }
                    JBInsets(top, 0, bottom, 0)
                }
            }

            override fun isBorderOpaque(): Boolean {
                return false
            }
        }
        myScrollPane.getViewport().border = null
        myScrollPane.getViewport().isOpaque = false
        myScrollPane.getHorizontalScrollBar().putClientProperty(JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, java.lang.Boolean.TRUE)
        myScrollPane.setOpaque(false)
        myClearButton = MyActionButton(ClearAction(), false, false)
        myNewLineButton = MyActionButton(NewLineAction(), false, true)
        myAddSelectedLinesCB = JCheckBox(CodifyBundle.message("aiToolbox.panes.chat.alsoSend")).apply {
            isOpaque = false
            addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent?) {
                    textArea.requestFocus()
                    textArea.dispatchEvent(e)
                }

                override fun keyPressed(e: KeyEvent?) {
                    textArea.requestFocus()
                    textArea.dispatchEvent(e)
                }

                override fun keyReleased(e: KeyEvent?) {
                    textArea.requestFocus()
                    textArea.dispatchEvent(e)
                }

            })
        }
        updateLayout()
    }

    fun setMultilineEnabled(enabled: Boolean) {
        if (myMultilineEnabled == enabled) return
        myMultilineEnabled = enabled
        textArea.document.putProperty("filterNewlines", if (myMultilineEnabled) null else java.lang.Boolean.TRUE)
        if (!myMultilineEnabled) {
            textArea.inputMap.put(KeyStroke.getKeyStroke("shift UP"), "selection-begin-line")
            textArea.inputMap.put(KeyStroke.getKeyStroke("shift DOWN"), "selection-end-line")
            textArea.addKeyListener(myEnterRedispatcher)
        } else {
            textArea.inputMap.put(KeyStroke.getKeyStroke("shift UP"), "selection-up")
            textArea.inputMap.put(KeyStroke.getKeyStroke("shift DOWN"), "selection-down")
            textArea.removeKeyListener(myEnterRedispatcher)
        }
        updateIconsLayout()
    }

    override fun getMinimumSize(): Dimension {
        return preferredSize
    }

    var needToInline: Boolean
        get() : Boolean {
            return myAddSelectedLinesCB.isSelected
        }
        set(value) {
            if (value != myAddSelectedLinesCB.isSelected) {
                myAddSelectedLinesCB.isSelected = value
                updateIconsLayout()
            }
        }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if ("background" == evt.propertyName) {
            repaint()
        }
        if ("font" == evt.propertyName) {
            updateLayout()
        }
    }

    @Deprecated("use this wrapper component with JBTextArea and its getEmptyText() instead")
    fun setInfoText(@Suppress("unused") info: String?) {
    }

    private inner class ClearAction internal constructor() : DumbAwareAction(CLOSE_ICON) {
        init {
            templatePresentation.hoveredIcon = CLOSE_HOVERED_ICON
        }

        override fun actionPerformed(e: AnActionEvent) {
            textArea.putClientProperty(JUST_CLEARED_KEY, !textArea.text.isEmpty())
            textArea.text = ""
        }
    }

    private inner class NewLineAction internal constructor() : DumbAwareAction(FindBundle.message("find.new.line"), null, AllIcons.Actions.SearchNewLine) {
        init {
            shortcutSet = CustomShortcutSet(NEW_LINE_KEYSTROKE)
            templatePresentation.hoveredIcon = AllIcons.Actions.SearchNewLineHover
        }

        override fun actionPerformed(e: AnActionEvent) {
            InsertBreakAction().actionPerformed(ActionEvent(textArea, 0, "action"))
        }
    }

    private class PseudoSeparatorBorder : Border {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            g.color = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
            g.fillRect(x + JBUI.scale(1), y + 1, 1, JBUI.scale(20))
        }

        override fun getBorderInsets(c: Component): Insets {
            return JBInsets(0, 7, 0, 0)
        }

        override fun isBorderOpaque(): Boolean {
            return false
        }
    }

    companion object {
        private val BACKGROUND_COLOR = JBColor.namedColor("Editor.SearchField.background", UIUtil.getTextFieldBackground())
        const val JUST_CLEARED_KEY = "JUST_CLEARED"
        val NEW_LINE_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)
        private val CLOSE_ICON = AllIcons.Actions.Close
        private val CLOSE_HOVERED_ICON = AllIcons.Actions.CloseHovered
        private val EMPTY_SCROLL_BORDER: Border = JBUI.Borders.empty(2, 6, 2, 2)
    }
}
