# 低功耗蓝牙API原生插件Ace
基于
[sand-plugin-bluetooth](https://ext.dcloud.net.cn/plugin?id=8551)开发。增加了如下新功能。
- 新功能：获取MTU值。通过 `getConnectedBluetoothDevices`返回的信息中拿到mtu值。20为代码中的默认值。IOS/Android均会在建立连接后尝试获取MTU最大值，IOS无法设置只能获取。Android设备注意的是：受到老版本Android设备和硬件本身的影响，将会在`确认连接稳定后`才会去自动设置MTU最大值。连接稳定确认过程需要一定的时间，从几百毫秒到几秒不等。开发者在获取Android设备MTU的时候要注意做滞后性获取或者尝试间隔一定的时间多尝试获取几次，以此来获取相对准确的MTU值。
- 新功能：支持无响应写。写特征数据时支持设备无响应写（writeNoResponse）。获取特征属性的时候将会返回设备writeNoResponse、signedWrite属性。

# Uni插件市场地址
https://ext.dcloud.net.cn/plugin?id=19527

# 功能列表：
- 初始化蓝牙模块(openBluetoothAdapter)
- 监听蓝牙适配器状态变化事件(onBluetoothAdapterStateChange)
- 开始搜寻附近的蓝牙外围设备(startBluetoothDevicesDiscovery)
- 停止搜寻附近的蓝牙外围设备(stopBluetoothDevicesDiscovery)
- 监听寻找到新设备的事件(onBluetoothDeviceFound)
- 连接低功耗蓝牙设备。(createBLEConnection)
- 断开与低功耗蓝牙设备的连接(closeBLEConnection)
- 监听低功耗蓝牙连接状态的改变事件。包括开发者主动连接或断开连接，设备丢失，连接异常断开等等(onBLEConnectionStateChange)
- 获取蓝牙设备所有服务(getBLEDeviceServices)
- 获取蓝牙设备某个服务中所有特征值(getBLEDeviceCharacteristics)
- 向低功耗蓝牙设备特征值中写入二进制数据(writeBLECharacteristicValue)
- 读取低功耗蓝牙设备的特征值的二进制数据值(readBLECharacteristicValue)
- 监听低功耗蓝牙设备的特征值变化事件(onBLECharacteristicValueChange)
- 添加断线重连设备(addAutoReconnect)
- 移除断线重连设备(removeAutoReconnect)
- 启用低功耗蓝牙设备特征值变化时的 notify 功能，订阅特征值(notifyBLECharacteristicValueChange)
- 取消订阅(cancelNotifyBLECharacteristicValueChange)
- 获取当前信号强度(getBLEDeviceRSSI)
- 获取已连接的设备列表(getConnectedBluetoothDevices)
- 获取已扫描到的所有设备(getBluetoothDevices)
- 获取蓝牙适配器最新状态(getBluetoothAdapterState)
- 关闭蓝牙适配器(closeBluetoothAdapter)

# 代码示例
```javascript
<script>
	const ble = uni.requireNativePlugin('sand-plugin-bluetooth-ace');
	var _this;
	export default {
		data() {
			return {
				title: 'Hello',
				msg:'还未开始',
        log:'',
				deviceList:[],
				deviceMap:{},
        toBottom:''
			}
		},
		onLoad() {
			_this=this;
			this.initListener();
		},
		methods: {
			initListener(){
				//适配器监听
				ble.onBluetoothAdapterStateChange({},(res)=>{
					console.log('适配器状态变化',res);
          _this.msg=res.message;
          _this.addLog(res.message);
				});
				//发现设备监听
				ble.onBluetoothDeviceFound({},(res)=>{
					console.log('发现了设备',res);
					if(res.status=='2500'){
						let devices=JSON.parse(res.devices);
						_this.addFindDevice(devices);
					}
				});
        //监听设备连接监听
        ble.onBLEConnectionStateChange({},(res)=>{
          let deviceId=res.deviceId;
          let connected=res.connected;
          let list=[..._this.deviceList];
          let dev=list[_this.deviceMap[deviceId]];
          dev.connected=connected;
          _this.deviceList=list;
          _this.addLog(dev.name+"连接状态="+connected);
          console.log(dev.name+"连接状态="+connected);
          //连接成功，获取服务和订阅特征
          if(connected==true){
            _this.notify(deviceId);
          }
        });
        //监听特征数据
        ble.onBLECharacteristicValueChange({},(res)=>{
          _this.addLog('十六进制值:'+res.value);
          console.log('十六进制值:'+res.value);
          let value=res.value;
          _this.addLog('解析后的值:'+JSON.stringify(_this.hex2Bytes(value)));
          console.log('解析后的值:'+JSON.stringify(_this.hex2Bytes(value)));
        });
				//打开适配器
				ble.openBluetoothAdapter({},(res)=>{
					console.log(res);
					_this.msg=res.message;
          _this.addLog(res.message);
				});
				
			},
      //启动扫描
      startScan(){
        ble.startBluetoothDevicesDiscovery({},(res)=>{
          _this.msg=res.message;
        });
      },
      //停止扫描
      stopScan(){
        ble.stopBluetoothDevicesDiscovery({},(res)=>{
          _this.msg=res.message;
          _this.addLog(res.message);
        });
      },
      //连接设备
      toConnect(deviceId){
        //停止扫描
        ble.stopBluetoothDevicesDiscovery({},(res)=>{
          _this.addLog(res.message);
        });
        //开始连接
        ble.createBLEConnection({deviceId:deviceId},(res)=>{
          _this.addLog(res.message);
          //加入自动重连
          ble.addAutoReconnect({deviceId:deviceId},(res2)=>{
            _this.addLog(res2.message);
          });
        });
      },
      //断开连接
      disConnect(deviceId){
        ble.removeAutoReconnect({deviceId:deviceId},(res)=>{
          _this.addLog(res.message);
          ble.closeBLEConnection({deviceId:deviceId},(res2)=>{
            _this.addLog(res2.message);
          });
        });
        
      },
      //刷新服务和订阅特征
      notify(deviceId){
        let timeout=0;
        setTimeout(()=>{
          //获取服务列表
          ble.getBLEDeviceServices({deviceId:deviceId},(res)=>{
            console.log(res);
            if(res.status=='2500'){
              let services=JSON.parse(res.services);
              _this.addLog(res.message+","+res.services);
              //由于是异步方法，采用递归进行遍历
              var diguiServices=function(serArr,index){
                ble.getBLEDeviceCharacteristics({
                  deviceId: deviceId,
                  serviceId: serArr[index].uuid
                  },(res2)=>{
                    _this.addLog(res2.message+","+res2.characteristics);
                    let characteristics=JSON.parse(res2.characteristics);
                    //订阅设备特征
                    if(serArr[index].uuid.indexOf('1905')>=0){
                      for (let j = 0; j < characteristics.length; j++) {
                        let characteristic=characteristics[j];
                        if(characteristic.properties.notify==true){
                          ble.notifyBLECharacteristicValueChange({
                            deviceId:deviceId,
                            serviceId:serArr[index].uuid,
                            characteristicId:characteristic.uuid
                          },(res3)=>{
                            _this.addLog(res3.message);
                          });
                        }
                        //读特征值
                        if(characteristic.properties.read==true){
                          ble.readBLECharacteristicValue({
                            deviceId:deviceId,
                            serviceId:serArr[index].uuid,
                            characteristicId:characteristic.uuid
                          },(res3)=>{
                            _this.addLog(res3.message);
                          });
                        }
                        //写一个值过去
                        if(characteristic.properties.write==true){
                          ble.writeBLECharacteristicValue({
                            deviceId:deviceId,
                            serviceId:serArr[index].uuid,
                            characteristicId:characteristic.uuid,
                            value:"01"//十六进制数据
                          },(res3)=>{
                            _this.addLog(res3.message);
                          });
                        }
                      }
                    }
                    
                    
                    let index2=index+1;
                    if(index2>=serArr.length){
                      return;
                    }else{
                      // console.log("递归下一个服务的特征值");
                      diguiServices(serArr,index2);
                    }
                  });
              };
              //开始遍历服务
              diguiServices(services,0);
            }else{
              _this.addLog(res.message);
            }
          });
        },timeout);
      },
      getAllScan(){
        //获取已经扫描到的所有设备
        ble.getBluetoothDevices({},(res)=>{
          _this.addLog('获取所有:'+res.devices);
          let arr=JSON.parse(res.devices);
          _this.addFindDevice(arr);
          for(let i=0; i<arr.length; i++){
            let dev=arr[i];
            if(dev.restore){
              _this.addLog('准备恢复这台设备'+dev.name);
              _this.toConnect(dev.deviceId);
            }
          }
        });
      },
      getConnected(){
        //获取已连接的所有设备
        ble.getConnectedBluetoothDevices({},(res)=>{
          _this.addLog('获取已连:'+res.devices);
        });
      },
      getStatus(){
        //获取当前适配器状态
        ble.getBluetoothAdapterState({},(res)=>{
          _this.addLog('获取状态:'+JSON.stringify(res));
        });
      },
      addLog(msg){
        _this.log=_this.log+new Date().toLocaleString()+":"+msg+"\r\n";
        _this.toBottom='';
        setTimeout(()=>{
          _this.toBottom="bottomId";
        },200);
      },
      addFindDevice(devices){
        let list=[..._this.deviceList];
        for(let i=0; i<devices.length ; i++){
          let dev=devices[i];
          //去重
          if(_this.deviceMap[dev.deviceId]>=0){
            let index=_this.deviceMap[dev.deviceId];
            //更新
            list.splice(index,1,dev);
          }else{
            //新增
            list.push(dev);
            _this.deviceMap[dev.deviceId]=list.length-1;
          }
        }
        _this.deviceList=list;
      },
      //十六进制字符串转数组
      hex2Bytes(hexStr){
        var pos = 0;
        var len = hexStr.length;
        if (len % 2 != 0) {
          return null;
        }
        len /= 2;
        var hexA = new Array();
        for (var i = 0; i < len; i++) {
          var s = hexStr.substr(pos, 2);
          var v = parseInt(s, 16);
          hexA.push(v);
          pos += 2;
        }
        return hexA;
      }
		}
	}
</script>
```
