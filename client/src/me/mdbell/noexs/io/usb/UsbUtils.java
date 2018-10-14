package me.mdbell.noexs.io.usb;

import javax.usb.*;
import java.util.List;

public final class UsbUtils {

    private UsbUtils(){

    }

    public static UsbDevice findDevice(int vendorId, int productId) throws UsbException {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub rootHub = services.getRootUsbHub();
        return findDevice(rootHub, (short)vendorId, (short)productId);
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
}
