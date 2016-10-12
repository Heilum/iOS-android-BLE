//
//  ViewController.swift
//  iOS_Bluetooth_peripheral
//
//  Created by CHENWANFEI on 10/10/2016.
//  Copyright © 2016 SwordFish. All rights reserved.
//

import UIKit
import CoreBluetooth



let g_periphral_service_uuid = "14FB2349-72FE-4CA2-94D6-1F3CB16331EE"
let g_periphral_characteristic_uuid = "1A3E4B28-522D-4B3B-82A9-D5E2004534FC"
let g_periphral_characteristic_uuid_to_be_written = "2A3E4B28-522D-4B3B-82A9-D5E2004534FC"
let g_periphral_characteristic_value = "Original Jagie Peripheral"



class ViewController: UIViewController {

    @IBOutlet weak var textViewLogger: UITextView!
    fileprivate  var peripheralManager: CBPeripheralManager!
    
    fileprivate var characteristic:CBMutableCharacteristic?
    fileprivate var characteristicToBeWritten:CBMutableCharacteristic?
    
    fileprivate var sequence = 0;
    
    fileprivate var timer:Timer?
    
    

    
    
    fileprivate var valueForCharacteristic:Data{
        return "value_\(self.sequence)".data(using: String.Encoding.utf8)!;
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    @IBAction func onStartPeripheral(_ sender: AnyObject) {
        if self.peripheralManager == nil {
            peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        }
    }
    
    func stopPeripheral(){
        peripheralManager?.stopAdvertising();
        peripheralManager = nil;
        characteristic = nil;
        
        self.timer?.invalidate();
        self.timer = nil;
    }
    
    func appendLog(log:String){
        self.textViewLogger.text = self.textViewLogger.text +  "\n" + log;
        let bottom = NSMakeRange(self.textViewLogger.text.characters.count - 1, 1);
        self.textViewLogger.scrollRangeToVisible(bottom);
    }

}


extension ViewController:CBPeripheralManagerDelegate{
    
    
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager)
    {
        if peripheral.state == .poweredOn{
            let advertisementData = [CBAdvertisementDataServiceUUIDsKey: [CBUUID(string:g_periphral_service_uuid)],CBAdvertisementDataLocalNameKey : "Jagie's iPhone5"] as [String : Any]
            peripheralManager.startAdvertising(advertisementData)
        }else{
            let log  = "peripheralManagerDidUpdateState,state = \(peripheral.state.rawValue)"
            self.appendLog(log: log);
            self.stopPeripheral();
        }
      
    }
    func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?)
    {
        if let error = error {
            let log  = "peripheralManagerDidStartAdvertising,error = \(error)"
            self.appendLog(log: log);
            
            stopPeripheral();
            
        }else{
            let log  = "peripheralManagerDidStartAdvertising,Success"
            self.appendLog(log: log);
            
            //for read
            let serviceUUID = CBUUID(string: g_periphral_service_uuid)
            let service = CBMutableService(type: serviceUUID, primary: true)
            
            let characteristicUUID = CBUUID(string: g_periphral_characteristic_uuid)
            let properties: CBCharacteristicProperties = [.notify, .read]
            let permissions: CBAttributePermissions = [.readable]
            let characteristic = CBMutableCharacteristic(
                type: characteristicUUID,
                properties: properties,
                value: nil,
                permissions: permissions)
            
            self.characteristic = characteristic;
            
            
            
            let characteristicUUIDForWriting = CBUUID(string: g_periphral_characteristic_uuid_to_be_written)
            let propertiesForWritting: CBCharacteristicProperties = [.notify, .read, .write]
            let permissionsForWriting: CBAttributePermissions = [.readable, .writeable]
            let characteristicForWriting = CBMutableCharacteristic(
                type: characteristicUUIDForWriting,
                properties: propertiesForWritting,
                value: nil,
                permissions: permissionsForWriting)
            self.characteristicToBeWritten = characteristicForWriting;
            
            service.characteristics = [self.characteristic!,self.characteristicToBeWritten!];
            
            peripheralManager.add(service)
            
            
         
            
          
        }
      
    }
    
    func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?)
    {
        if let error = error {
            let log  = "didAddService,error = \(error)"
            self.appendLog(log: log);
            
            stopPeripheral();
            return
        }
        let log  = "didAddService,Success,waiting for read"
        self.appendLog(log: log);
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: {[weak self] (t) in
            guard let `self` = self else{
                return;
            }
            
            
            self.sequence = self.sequence + 1;
           
            if let c = self.characteristic{
                c.value = self.valueForCharacteristic;
            }
            peripheral.updateValue(self.valueForCharacteristic, for: self.characteristic!, onSubscribedCentrals: nil);
            
            
        })
        
      
    }
    
    
    
    
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest){
        if let characteristic = characteristic, request.characteristic.uuid.isEqual(characteristic.uuid){
            // Set the correspondent characteristic's value
            // to the request
            request.value = self.valueForCharacteristic;

            // Respond to the request
            peripheralManager.respond(
                to: request,
                withResult: .success)
            
            
            if let data = request.value,let valueString = String(data: data, encoding: String.Encoding.utf8){
                let log  = "send value =\(valueString)"
                self.appendLog(log: log);
            }
            
           

        }
    }
    
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]){
        for request in requests
        {
            if request.characteristic.uuid.isEqual(self.characteristicToBeWritten?.uuid)
            {
                // Set the request's value
                // to the correspondent characteristic
                //characteristic?.value = request.value
                
                let value = String(data: request.value!, encoding: String.Encoding.utf8)!;
                
                self.appendLog(log: ">>>>Central write value:\(value)");
            }
        }
        peripheralManager.respond(to: requests[0], withResult: .success)
    }
    
    func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager){
        self.appendLog(log: "peripheralManagerIsReady toUpdateSubscribers");

    }
    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic){
        self.appendLog(log: "didSubscribeTo characteristic");
    }
    
}
