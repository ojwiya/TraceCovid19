//
//  Beacon.swift
//  tracer
//
//  Created by stl-037 on 26/03/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

import Foundation

@objc(Beacon)
class Beacon: NSObject {
  
  @objc
  static func requiresMainQueueSetup() -> Bool {
    return true
  }
  
  
  @objc
  func startBroadcast(uuid: String) -> Void {
    print("uuid ", uuid)
  }
  
  @objc(startScanning:)
   func startScanning(uuid: String) -> Void {
     print("uuid ", uuid)
   }
}