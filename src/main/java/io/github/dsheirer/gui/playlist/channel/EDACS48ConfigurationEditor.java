/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channelMap.ViewChannelMapEditorRequest;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.record.RecordConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.edacs48.DecodeConfigEDACS48;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MAXIMUM;
import static io.github.dsheirer.module.decode.config.DecodeConfiguration.CALL_TIMEOUT_MINIMUM;

/**
 * EDACS48 channel configuration editor
 */
public class EDACS48ConfigurationEditor extends ChannelConfigurationEditor
{
    private TitledPane mDecoderPane;
    private TitledPane mEventLogPane;
    private TitledPane mRecordPane;
    private TitledPane mSourcePane;
    private SourceConfigurationEditor<SourceConfiguration> mSourceConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private RecordConfigurationEditor mRecordConfigurationEditor;
    private ComboBox<ChannelMap> mChannelMapComboBox;
    private Button mChannelMapEditButton;
    private Spinner<Integer> mTrafficChannelPoolSizeSpinner;
    private Spinner<Integer> mCallTimeoutSpinner;

    public EDACS48ConfigurationEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        super(playlistManager, userPreferences);
        getTitledPanesBox().getChildren().add(getSourcePane());
        getTitledPanesBox().getChildren().add(getDecoderPane());
        getTitledPanesBox().getChildren().add(getEventLogPane());
        getTitledPanesBox().getChildren().add(getRecordPane());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.EDACS48;
    }

    private TitledPane getSourcePane()
    {
        if(mSourcePane == null)
        {
            mSourcePane = new TitledPane("Source", getSourceConfigurationEditor());
            mSourcePane.setExpanded(true);
        }

        return mSourcePane;
    }

    private TitledPane getDecoderPane()
    {
        if(mDecoderPane == null)
        {
            mDecoderPane = new TitledPane();
            mDecoderPane.setText("Decoder: EDACS Narrowband");
            mDecoderPane.setExpanded(true);

            GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(10,10,10,10));
            gridPane.setHgap(10);
            gridPane.setVgap(10);

            Label channelMapLabel = new Label("Channel Map");
            GridPane.setHalignment(channelMapLabel, HPos.RIGHT);
            GridPane.setConstraints(channelMapLabel, 0, 0);
            gridPane.getChildren().add(channelMapLabel);

            GridPane.setConstraints(getChannelMapComboBox(), 1, 0, 2, 1,
                HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES);
            gridPane.getChildren().add(getChannelMapComboBox());

            GridPane.setConstraints(getChannelMapEditButton(), 3, 0);
            gridPane.getChildren().add(getChannelMapEditButton());

            Label poolSizeLabel = new Label("Max Traffic Channels");
            GridPane.setHalignment(poolSizeLabel, HPos.RIGHT);
            GridPane.setConstraints(poolSizeLabel, 0, 1);
            gridPane.getChildren().add(poolSizeLabel);

            GridPane.setConstraints(getTrafficChannelPoolSizeSpinner(), 1, 1);
            gridPane.getChildren().add(getTrafficChannelPoolSizeSpinner());

            Label callTimeoutLabel = new Label("Call Timeout Seconds");
            GridPane.setHalignment(callTimeoutLabel, HPos.RIGHT);
            GridPane.setConstraints(callTimeoutLabel, 2, 1);
            gridPane.getChildren().add(callTimeoutLabel);

            GridPane.setConstraints(getCallTimeoutSpinner(), 3, 1);
            gridPane.getChildren().add(getCallTimeoutSpinner());

            mDecoderPane.setContent(gridPane);
        }

        return mDecoderPane;
    }

    private TitledPane getEventLogPane()
    {
        if(mEventLogPane == null)
        {
            mEventLogPane = new TitledPane("Logging", getEventLogConfigurationEditor());
            mEventLogPane.setExpanded(false);
        }

        return mEventLogPane;
    }

    private TitledPane getRecordPane()
    {
        if(mRecordPane == null)
        {
            mRecordPane = new TitledPane();
            mRecordPane.setText("Recording");
            mRecordPane.setExpanded(false);

            Label notice = new Label("Note: use aliases to control call audio recording");
            notice.setPadding(new Insets(10, 10, 0, 10));

            VBox vBox = new VBox();
            vBox.getChildren().addAll(getRecordConfigurationEditor(), notice);

            mRecordPane.setContent(vBox);
        }

        return mRecordPane;
    }

    private SourceConfigurationEditor<SourceConfiguration> getSourceConfigurationEditor()
    {
        if(mSourceConfigurationEditor == null)
        {
            mSourceConfigurationEditor = new FrequencyEditor(getTunerModel());

            // Add a listener so that we can push change notifications up to this editor
            mSourceConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSourceConfigurationEditor;
    }

    private EventLogConfigurationEditor getEventLogConfigurationEditor()
    {
        if(mEventLogConfigurationEditor == null)
        {
            List<EventLogType> types = new ArrayList<>();
            types.add(EventLogType.CALL_EVENT);
            types.add(EventLogType.DECODED_MESSAGE);
            types.add(EventLogType.TRAFFIC_CALL_EVENT);
            types.add(EventLogType.TRAFFIC_DECODED_MESSAGE);

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

    private RecordConfigurationEditor getRecordConfigurationEditor()
    {
        if(mRecordConfigurationEditor == null)
        {
            List<RecorderType> types = new ArrayList<>();
            types.add(RecorderType.BASEBAND);
            types.add(RecorderType.DEMODULATED_BIT_STREAM);
            types.add(RecorderType.TRAFFIC_BASEBAND);
            types.add(RecorderType.TRAFFIC_DEMODULATED_BIT_STREAM);
            mRecordConfigurationEditor = new RecordConfigurationEditor(types);
            mRecordConfigurationEditor.setDisable(true);
            mRecordConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mRecordConfigurationEditor;
    }

    private ComboBox<ChannelMap> getChannelMapComboBox()
    {
        if(mChannelMapComboBox == null)
        {
            mChannelMapComboBox = new ComboBox<>();
            mChannelMapComboBox.setMaxWidth(Double.MAX_VALUE);
            mChannelMapComboBox.setDisable(true);
            mChannelMapComboBox.setTooltip(new Tooltip("Select a channel map to use for this system"));
            mChannelMapComboBox.setItems(getPlaylistManager().getChannelMapModel().getChannelMaps());
            mChannelMapComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mChannelMapComboBox;
    }

    private Button getChannelMapEditButton()
    {
        if(mChannelMapEditButton == null)
        {
            mChannelMapEditButton = new Button("Channel Map Editor");
            mChannelMapEditButton.setTooltip(new Tooltip("Open the channel map editor to add/edit maps"));
            mChannelMapEditButton.setOnAction(event -> {
                final var selectedItem = getChannelMapComboBox().getSelectionModel().getSelectedItem();
                final var channelMapName = selectedItem != null ? selectedItem.getName() : null;
                MyEventBus.getGlobalEventBus().post(new ViewChannelMapEditorRequest(channelMapName));
            });
        }

        return mChannelMapEditButton;
    }

    private Spinner<Integer> getTrafficChannelPoolSizeSpinner()
    {
        if(mTrafficChannelPoolSizeSpinner == null)
        {
            mTrafficChannelPoolSizeSpinner = new Spinner<>();
            mTrafficChannelPoolSizeSpinner.setDisable(true);
            mTrafficChannelPoolSizeSpinner.setTooltip(
                new Tooltip("Maximum number of traffic channels that can be created by the decoder"));
            mTrafficChannelPoolSizeSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            final var svf = new IntegerSpinnerValueFactory(0, 50);
            mTrafficChannelPoolSizeSpinner.setValueFactory(svf);
            mTrafficChannelPoolSizeSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mTrafficChannelPoolSizeSpinner;
    }

    private Spinner<Integer> getCallTimeoutSpinner()
    {
        if(mCallTimeoutSpinner == null)
        {
            mCallTimeoutSpinner = new Spinner<>();
            mCallTimeoutSpinner.setDisable(true);
            mCallTimeoutSpinner.setTooltip(new Tooltip("Maximum call limit in seconds"));
            mCallTimeoutSpinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
            final var svf = new IntegerSpinnerValueFactory(CALL_TIMEOUT_MINIMUM, CALL_TIMEOUT_MAXIMUM);
            mCallTimeoutSpinner.setValueFactory(svf);
            mCallTimeoutSpinner.getValueFactory().valueProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mCallTimeoutSpinner;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        getChannelMapComboBox().setDisable(config == null);
        getCallTimeoutSpinner().setDisable(config == null);
        getTrafficChannelPoolSizeSpinner().setDisable(config == null);

        getChannelMapComboBox().getSelectionModel().select(null);

        if(config instanceof DecodeConfigEDACS48)
        {
            final var edacsConfig = (DecodeConfigEDACS48) config;

            final var channelMapName = edacsConfig.getChannelMapName();
            if(channelMapName != null)
            {
                for(ChannelMap channelMap : getChannelMapComboBox().getItems())
                {
                    if(channelMap.getName().contentEquals(channelMapName))
                    {
                        getChannelMapComboBox().getSelectionModel().select(channelMap);
                    }
                }
            }

            int callTimeoutSeconds = edacsConfig.getCallTimeoutSeconds();
            getCallTimeoutSpinner().getValueFactory().setValue(callTimeoutSeconds);

            int channelPoolSize = edacsConfig.getTrafficChannelPoolSize();
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(channelPoolSize);
        }
        else if(config != null)
        {
            getCallTimeoutSpinner().getValueFactory().setValue(DecodeConfigEDACS48.DEFAULT_CALL_TIMEOUT_DELAY_SECONDS);
            getTrafficChannelPoolSizeSpinner().getValueFactory().setValue(DecodeConfigEDACS48.TRAFFIC_CHANNEL_LIMIT_DEFAULT);
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigEDACS48 config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigEDACS48)
        {
            config = (DecodeConfigEDACS48) getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigEDACS48();
        }

        config.setCallTimeoutSeconds(getCallTimeoutSpinner().getValue());
        config.setTrafficChannelPoolSize(getTrafficChannelPoolSizeSpinner().getValue());

        final var selectedChannelMap = getChannelMapComboBox().getSelectionModel().getSelectedItem();
        config.setChannelMapName(selectedChannelMap != null ? selectedChannelMap.getName() : null);

        getItem().setDecodeConfiguration(config);
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
        getEventLogConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveEventLogConfiguration()
    {
        getEventLogConfigurationEditor().save();

        if(getEventLogConfigurationEditor().getItem().getLoggers().isEmpty())
        {
            getItem().setEventLogConfiguration(null);
        }
        else
        {
            getItem().setEventLogConfiguration(getEventLogConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config)
    {
        //no-op
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
        //no-op
    }

    @Override
    protected void setRecordConfiguration(RecordConfiguration config)
    {
        getRecordConfigurationEditor().setDisable(config == null);
        getRecordConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveRecordConfiguration()
    {
        getRecordConfigurationEditor().save();
        RecordConfiguration config = getRecordConfigurationEditor().getItem();
        getItem().setRecordConfiguration(config);
    }

    @Override
    protected void setSourceConfiguration(SourceConfiguration config)
    {
        getSourceConfigurationEditor().setSourceConfiguration(config);
    }

    @Override
    protected void saveSourceConfiguration()
    {
        getSourceConfigurationEditor().save();
        SourceConfiguration sourceConfiguration = getSourceConfigurationEditor().getSourceConfiguration();
        getItem().setSourceConfiguration(sourceConfiguration);
    }
}
