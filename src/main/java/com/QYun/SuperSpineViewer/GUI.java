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
            // The label formatter was not working until JavaFX 8.
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

            glInfoLabel.setText(vendor + " OpenGL " + version);

            renderChoice.setItems(observableList(renderStreamFactories));
            for (int i = 0; i < renderStreamFactories.size(); i++)
            {

                if (renderStreamFactories.get(i) == graphics1.getRenderStreamFactory())
                {

                    renderChoice.getSelectionModel().select(i);
                    break;
                }
            }
            renderChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> graphics1.setRenderStreamFactory(newValue));

            textureChoice.setItems(observableList(textureStreamFactories));
            for (int i = 0; i < textureStreamFactories.size(); i++)
            {
                if (graphics1 != null && textureStreamFactories.get(i) == graphics1.getTextureStreamFactory())
                {
                    textureChoice.getSelectionModel().select(i);
                    break;
                }
            }

            textureChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> graphics1.setTextureStreamFactory(newValue));

            bufferingChoice.getSelectionModel().select(graphics1.getTransfersToBuffer() - 1);
            bufferingChoice.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> graphics1.setTransfersToBuffer(newValue.getTransfersToBuffer()));

            vsync.selectedProperty().addListener((observableValue, oldValue, newValue) -> graphics1.setVSync(newValue));

            final int maxSamples = graphics1.getMaxSamples();
            if (maxSamples == 1)
                msaaSamples.setDisable(true);
            else
            {
                msaaSamples.setMax(Integer.numberOfTrailingZeros(maxSamples));
                msaaSamples.valueProperty().addListener((observableValue, oldValue, newValue) -> {

                    int samples = 1 << newValue.intValue();
                    graphics1.setSamples(samples);
                });
            }
        });

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

}
