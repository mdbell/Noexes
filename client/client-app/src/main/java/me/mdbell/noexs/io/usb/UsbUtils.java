package me.mdbell.noexs.io.usb;

import javax.usb.*;
import java.util.List;

public final class UsbUtils {

    private static final int SWITCH_VENDOR_ID = 0x57E;
    private static final int SWITCH_PROD_ID = 0x3000;

    private UsbUtils(){

    }

    public static UsbDevice findSwitch() throws UsbException {
        return findDevice(SWITCH_VENDOR_ID, SWITCH_PROD_ID);
    }

    public static UsbDevice findDevice(int vendorId, int productId) throws UsbException {
        return findDevice(getRootHub(), (short)vendorId, (short)productId);
    }

    //Adapted from usb4java examples
    public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if (device.isUsbHub())
            {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        return null;
    }

    public static UsbHub getRootHub() throws UsbException {
        UsbServices services = UsbHostManager.getUsbServices();
        return services.getRootUsbHub();
    }

    public static boolean isSwitch(UsbDevice d) {
        UsbDeviceDescriptor desc = d.getUsbDeviceDescriptor();
        return desc.idProduct() == SWITCH_PROD_ID && desc.idVendor() == SWITCH_VENDOR_ID;
    }
}
