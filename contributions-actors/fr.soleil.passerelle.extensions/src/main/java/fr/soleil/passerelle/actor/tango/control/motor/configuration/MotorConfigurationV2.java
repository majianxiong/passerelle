package fr.soleil.passerelle.actor.tango.control.motor.configuration;

import static fr.esrf.Tango.DevState.ALARM;
import static fr.esrf.Tango.DevState.DISABLE;
import static fr.esrf.Tango.DevState.FAULT;
import static fr.esrf.Tango.DevState.MOVING;
import static fr.esrf.Tango.DevState.OFF;
import static fr.esrf.Tango.DevState.ON;
import static fr.esrf.Tango.DevState.STANDBY;
import static fr.esrf.Tango.DevState.UNKNOWN;
import static fr.soleil.passerelle.actor.tango.control.motor.configuration.InitType.OTHER;

import org.tango.utils.DevFailedUtils;

import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.PasserelleException;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.Except;
import fr.soleil.passerelle.actor.tango.control.motor.configuration.initDevices.Command;
import fr.soleil.passerelle.actor.tango.control.motor.configuration.initDevices.ErrorCommand;
import fr.soleil.passerelle.actor.tango.control.motor.configuration.initDevices.InitCommand;
import fr.soleil.passerelle.actor.tango.control.motor.configuration.initDevices.MicroCodeCommand;
import fr.soleil.passerelle.actor.tango.control.motor.configuration.initDevices.OnCommand;
import fr.soleil.passerelle.util.DevFailedProcessingException;
import fr.soleil.passerelle.util.ProcessingExceptionWithLog;
import fr.soleil.tango.clientapi.TangoCommand;
import fr.soleil.tango.clientapi.factory.ProxyFactory;

public class MotorConfigurationV2 {

    public static final String NO_CONTROL_BOX_ATTACHED_TO = "No control box attached to ";
    public static final String AXIS_ENCODER_TYPE_PROPERTY = "AxisEncoderType";
    public static final String AXIS_INIT_TYPE_PROPERTY = "AxisInitType";
    public static final String AXIS_INIT_POSITION_PROPERTY = "AxisInitPosition";
    public static final String AXIS_ENCODER_TYPE_PROPERTY_IS_NOT_INT = AXIS_ENCODER_TYPE_PROPERTY
            + " does not exist or is not an integer";
    public static final String AXIS_INIT_POSITION_PROPERTY_IS_NaN = AXIS_INIT_POSITION_PROPERTY
            + " does not exist or is not a number";
    private final String deviceName;
    private final DeviceProxy axisProxy;
    private final String controlBoxDeviceClass;
    private EncoderType encoder;
    private InitType initStrategy;
    /**
     * indicate if we have to switch the motor in off state after executing InitReferencePosition or
     * DefinePosition
     */
    private boolean switchToOffAfterInit;
    private String controlBoxName;

    /**
     * Retrieve the controlBox (cb) of one device To test this class, we don't use real cb so the
     * class device is not same than real. So we add a boolean to specify which class name should be
     * be used to find the cb
     * 
     * @param deviceName the motor that we want to find the cb
     * @param isTestEnv flag that indicate if we are un test or production environment
     * 
     * @throws fr.esrf.Tango.DevFailed if the deviceProxy to the motor can not be created of
     *             Devfailed is raised
     */
    public MotorConfigurationV2(final String deviceName, boolean isTestEnv) throws DevFailed {
        this.deviceName = deviceName;
        axisProxy = ProxyFactory.getInstance().createDeviceProxy(deviceName);
        controlBoxDeviceClass = isTestEnv ? "fr.soleil.deviceservers.simulated.SimulatedControlBox"
                : "ControlBox";
        switchToOffAfterInit = false;

    }

    /**
     * Retrieve the controlBox (cb) of one device in production environment. @see
     * MotorConfiguration(final String deviceName, boolean isTestEnv)
     */
    public MotorConfigurationV2(final String deviceName) throws DevFailed {
        this(deviceName, false);
    }

    public boolean isSwitchToOffAfterInit() {
        return switchToOffAfterInit;
    }

    /**
     * retrieve the controlbox associated to the motor and it characteristics (encoder, init
     * strategy, initPosition)
     * 
     * @throws DevFailed
     */
    public void retrieveFullConfig() throws DevFailed {
        retrieveMyControlBox();
        retrieveConfig();
    }

    /**
     * retrieve the motor characteristics (encoder, init strategy, initPosition)
     * 
     * @throws DevFailed
     */
    public void retrieveConfig() throws DevFailed {
        // TODO add AxisInitPosition (test is Number) ?
        final String[] props = { AXIS_ENCODER_TYPE_PROPERTY, AXIS_INIT_TYPE_PROPERTY };
        final DbDatum[] datum = axisProxy.get_property(props);

        try {
            encoder = EncoderType.getValueFromOrdinal(datum[0].extractLong());
        }
        catch (NumberFormatException e) {
            DevFailedUtils.throwDevFailed(AXIS_ENCODER_TYPE_PROPERTY_IS_NOT_INT);
        }
        initStrategy = InitType.getValueIfContains(datum[1].extractString());

        try {
            if (initStrategy == OTHER) {
                // initializeReference command is available. to the command works the
                // AxisInitPosition property must be a number.
                axisProxy.get_property(AXIS_INIT_POSITION_PROPERTY).extractDouble();
            }
        }
        catch (NumberFormatException e) {
            DevFailedUtils.throwDevFailed(AXIS_INIT_POSITION_PROPERTY_IS_NaN);
        }
    }

    /**
     * retrieve the controlBox associated to the device
     * 
     * @throws DevFailed
     */
    public void retrieveMyControlBox() throws DevFailed {
        String controlBoxName = null;
        final DeviceData dd = axisProxy.get_adm_dev().command_inout("QueryDevice");
        final String[] devices = dd.extractStringArray();
        for (final String device : devices) {
            final String[] classAndDevice = device.split("::");
            if (classAndDevice[0].equals(controlBoxDeviceClass)) {
                controlBoxName = classAndDevice[1];
                break;
            }
        }
        if (controlBoxName == null) {
            Except.throw_exception("TANGO_ERROR", NO_CONTROL_BOX_ATTACHED_TO + deviceName,
                    "MotorConfiguration.retrieveMyControlBox");
        }
        this.controlBoxName = controlBoxName;
    }

    public void initDevice(final Actor actor) throws ProcessingException {

        try {
            // 1 - Init the controlBox
            TangoCommand stateCmd = new TangoCommand(controlBoxName, "State");

            executeCmdAccordingState(new InitCommand(actor, controlBoxName, stateCmd), FAULT,
                    UNKNOWN);
            executeCmdAccordingState(new MicroCodeCommand(actor, controlBoxName, stateCmd), ALARM);

            // 2- Init the galil
            stateCmd = new TangoCommand(deviceName, "State");

            executeCmdAccordingState(new InitCommand(actor, deviceName, stateCmd), FAULT, UNKNOWN);
            executeCmdAccordingState(new ErrorCommand(actor, deviceName, stateCmd), MOVING, DISABLE);
            switchToOffAfterInit = executeCmdAccordingState(new OnCommand(actor, deviceName,
                    stateCmd), OFF);

            // checks galil is in expected state
            DevState galilState = stateCmd.execute(DevState.class);
            if (galilState != STANDBY && galilState != ON) {
                throw new ProcessingExceptionWithLog(actor, "Motor is " + galilState.toString()
                        + " insteadof  StandBy or On", this, null);
            }

        }
        catch (DevFailed e) {
            throw new DevFailedProcessingException(e, PasserelleException.Severity.FATAL, actor);
        }
    }

    /**
     * execute the command if the device is in particular state
     * 
     * @param command the command to execute
     * @param states the states which "trig" the command
     * 
     * @return true if an specific action must be executed after //TODO change this dirty hack
     * 
     * @throws DevFailed an tango error occure (timeout , bad device name...)
     * @throws ProcessingExceptionWithLog if the device is particular state after the execution of
     *             the command
     */
    private boolean executeCmdAccordingState(Command command, DevState... states) throws DevFailed,
            ProcessingExceptionWithLog {
        DevState deviceState = command.getStateCommand().execute(DevState.class);
        for (DevState state : states) {
            if (state == deviceState) {
                return command.execute(states);
            }
        }
        return false;
    }

    public EncoderType getEncoder() {
        return encoder;
    }

    public InitType getInitStrategy() {
        return initStrategy;
    }

    public String getControlBoxName() {
        return controlBoxName;
    }
}