# FE2_Monitoring

The purpose of this Java based tool is to monitor the infrastructure 
of our local fire department, especially the Alamos FE2-System.  
It checks infrastructure via Ping, HTTP and APIs.

## Implemented Checks
(this is also an example output on console and in pushover message)  
  
✅ FritzBox Ping  
✅ FritzBox WebUI  
❌ WindowsPC Ping: Unreachable  
✅ RaspberryPi Ping  
✅ FE2 directly  
✅ FE2 reverse proxy redirect  
✅ FE2 via reverse proxy  
✅ FE2 via web  
✅ [FE2_Kartengenerierung](https://github.com/FFW-Baudenbach/FE2_Kartengenerierung) health  
✅ FE2_Kartengenerierung generated maps  
✅ Printer Ping  
✅ Printer WebUI  
✅ Printer Device Status: running(2)  
✅ Printer Black Toner: 75% remaining  
✅ Website  
✅ Website redirect  

## Daily aliveness check

Sends once in the morning a message about the current state.

## Pushover notification

If something goes wrong more than 5 times, a Pushover notification is sent to the admin.  
If it recovered more than 5 times in a row, a resolved Pushover message is also sent to the admin.

## Note

The Tool is not generalized, all configurations (but secrets of course) are hardcoded.  
The timings can be configured as well or the defaults are used.  
  
The tool is designed to be as simple as possible for also not too experienced developers.

