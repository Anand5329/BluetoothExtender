## Extending the range of a Bluetooth Connection

This app was developed to turn an android device into a sort of bluetooth extender - enabling connected devices to be farther apart without losses/drops in the connection.

After considerable efforts, I have realised that this is not possible over the Bluetooth stack on Android without tinkering with how the stack is configured on an Android device. Essentially, the device would need to act as an A2DP sink
which means that it would need to be able to receive audio streams from other devices. However, this is not something that can be controlled via the Android Bluetooth API. To achieve this, you would require possibly a recompilation of the Android system.

See: https://stackoverflow.com/questions/27763756/android-device-as-a-receiver-for-a2dp-profile

## Workaround

Use Wi-Fi instead of Bluetooth!
Apps like AudioRelay do this: https://audiorelay.net/
