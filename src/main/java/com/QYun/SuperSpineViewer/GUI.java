package com.QYun.SuperSpineViewer;

import com.badlogic.gdx.backends.lwjgl.LwjglFXGraphics;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import org.lwjgl.util.stream.StreamUtil;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static javafx.collections.FXCollections.observableArrayList;
import static javafx.collections.FXCollections.observableList;
import static org.lwjgl.opengl.GL11.*;

public class GUI implements Initializable {

    @FXML
    public ResourceBundle resources;

    @FXML
    public URL location;

    @FXML
    public Label fpsLabel;

    @FXML
    public Font x5;

    @FXML
    public Label fpsLabel2;

    @FXML
    public Label glInfoLabel;

    @FXML
    public Font x41;

    @FXML
    public Label javaInfoLabel;

    @FXML
    public Label systemInfoLabel;

    @FXML
    public AnchorPane gearsRoot;

    @FXML
    public ImageView imgView1;

    @FXML
    public VBox vBox;

    @FXML
    public CheckBox vsync;

    @FXML
    public Font x11;

    @FXML
    public Color x21;

    @FXML
    public ChoiceBox<StreamUtil.RenderStreamFactory> renderChoice;

    @FXML
    public ChoiceBox<StreamUtil.TextureStreamFactory> textureChoice;

    @FXML
    public ChoiceBox<BufferingChoice> bufferingChoice;

    @FXML
    public Slider msaaSamples;

    @FXML
    public Font x3;

    @FXML
    public Color x4;

    public LwjglFXGraphics graphics1;

    public GUI()
    {
    }

    public void initialize(final URL url, final ResourceBundle resourceBundle)
    {
        final StringBuilder info = new StringBuilder(128);
        info.append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version")).append(' ').append(System.getProperty("java.vm.version"));
        javaInfoLabel.setText(info.toString());

        info.setLength(0);
        info.append(System.getProperty("os.name")).append(" - JavaFX ").append(System.getProperty("javafx.runtime.version"));
        systemInfoLabel.setText(info.toString());

        bufferingChoice.setItems(observableArrayList(BufferingChoice.values()));

        msaaSamples.setMin(0);
        msaaSamples.setMax(0);
        if (System.getProperty("javafx.runtime.version").startsWith("2"))
            for (Node n : msaaSamples.getParent().getChildrenUnmodifiable())
            {
                if (!(n instanceof Label))
                    continue;

                Label l = (Label) n;
                if ("MSAA Samples".equals(l.getText()))
                {
                    l.setText("MSAA Samples (2^x)");
                    break;
                }
            }
        else
            msaaSamples.setLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(final Double object) {
                    return Integer.toString(1 << object.intValue());
                }

                @Override
                public Double fromString(final String string) {
                    return null;
                }
            });
    }

    public void runGears()
    {
        final List<StreamUtil.RenderStreamFactory> renderStreamFactories = StreamUtil.getRenderStreamImplementations();
        final List<StreamUtil.TextureStreamFactory> textureStreamFactories = StreamUtil.getTextureStreamImplementations();
        final String vendor = glGetString(GL_VENDOR);
        final String version = glGetString(GL_VERSION);

        Platform.runLater(() -> {

            bufferingChoice.getSelectionModel().select(graphics1.getTransfersToBuffer() - 1);
            graphics1.setTransfersToBuffer(bufferingChoice.getSelectionModel().getSelectedItem().getTransfersToBuffer());

            textureChoice.setItems(observableList(textureStreamFactories));
            for (int i = 0; i < textureStreamFactories.size(); i++)
            {
                if (graphics1 != null && textureStreamFactories.get(i) == graphics1.getTextureStreamFactory())
                {
                    textureChoice.getSelectionModel().select(textureStreamFactories.size() - 1);
                    graphics1.setTextureStreamFactory(textureChoice.getSelectionModel().getSelectedItem());
                    break;
                }
            }

            final int maxSamples = Objects.requireNonNull(graphics1).getMaxSamples();
            if (maxSamples == 1)
                msaaSamples.setDisable(true);
            else
            {
                msaaSamples.setMax(Integer.numberOfTrailingZeros(maxSamples));
                msaaSamples.setValue(msaaSamples.getMax());
                graphics1.setSamples((int) msaaSamples.getMax());
                msaaSamples.valueProperty().addListener((observableValue, oldValue, newValue) -> ReFresh());
            }

            renderChoice.setItems(observableList(renderStreamFactories));
            for (int i = 0; i < renderStreamFactories.size(); i++)
            {
                if (renderStreamFactories.get(i) == graphics1.getRenderStreamFactory())
                {
                    renderChoice.getSelectionModel().select(renderStreamFactories.size() - 1);
                    graphics1.setRenderStreamFactory(renderChoice.getSelectionModel().getSelectedItem());
                    break;
                }
            }

            glInfoLabel.setText(vendor + " OpenGL " + version);
            vsync.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
                graphics1.setVSync(newValue);
                System.out.println("设置帧率同步：" + newValue);
            });
            renderChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> ReFresh());
            textureChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> ReFresh());
            bufferingChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> ReFresh());
        });
        Status();
    }

    private enum BufferingChoice
    {
        SINGLE(1, "No buffering"), DOUBLE(2, "Double buffering"), TRIPLE(3,
            "Triple buffering");

        private final int transfersToBuffer;
        private final String description;

        BufferingChoice(final int transfersToBuffer, final String description)
        {
            this.transfersToBuffer = transfersToBuffer;
            this.description = transfersToBuffer + "x - " + description;
        }

        public int getTransfersToBuffer()
        {
            return transfersToBuffer;
        }

        public String getDescription()
        {
            return description;
        }

        public String toString()
        {
            return description;
        }
    }

    private void ReFresh () {
        graphics1.setSamples((int) msaaSamples.getValue());
        graphics1.setTransfersToBuffer(bufferingChoice.getSelectionModel().getSelectedItem().getTransfersToBuffer());
        graphics1.setTextureStreamFactory(textureChoice.getSelectionModel().getSelectedItem());
        graphics1.setRenderStreamFactory(renderChoice.getSelectionModel().getSelectedItem());
        Status();
    }

    public void Status() {
        System.out.println("-----------------------------------");
        System.out.println("上一个流渲染：" + graphics1.getRenderStreamFactory());
        System.out.println("上一个贴图渲染：" + graphics1.getTextureStreamFactory());
        System.out.println("上一个缓冲区：" + graphics1.getTransfersToBuffer());
        System.out.println("-----------------------------------");
        if (renderChoice.getValue() != null) {
            System.out.println("当前流渲染：" + renderChoice.getValue());
            System.out.println("当前贴图渲染：" + textureChoice.getValue());
            System.out.println("当前缓冲区：" + bufferingChoice.getValue());
            System.out.println("当前超采样：" + msaaSamples.getValue());
            System.out.println("-----------------------------------");
        }
    }

}
