# RideRate v4

RideRate is an Android test app for manually analyzing visible Uber Ride and Delivery offers.

## Current workflow
1. Open RideRate.
2. Tap **Start RideRate**.
3. Enable RideRate Live Detection if Android requests it.
4. A movable RideRate bubble appears over other apps.
5. When an Uber offer is visible, tap the bubble.
6. RideRate takes one on-demand screenshot and OCRs the lower half of the screen.
7. It calculates an estimated hourly rate, stores the record privately, sends a normal notification, and displays an 80%-opaque result banner.

RideRate does not continuously OCR the screen.

## Calculation rules
### Ride
`adjusted total = pickup time + trip time + (stops × 2 minutes)`

The stop buffer is hard-coded at 2 minutes per stop.

### Delivery
`adjusted total = Uber displayed total + Delivery Wait Buffer`

Delivery Wait Buffer is configurable from 0–15 minutes and defaults to 2 minutes.

## History
History includes date filtering, four KPI metrics, a Bottom/Mid/Top donut chart, collapsible day groups, sorting, deletion tools, CSV export, private verification images, and explicit Ride Snapshot downloads.

History row format:
`Rate: $33.88/hr | Offer $13.55 / 24 min`

Rows wrap and expand if the device is too narrow for one line.

## Settings
- Auto-delete history on/off
- Configurable normal-history limit
- Delivery wait buffer (default 2 min)
- Banner duration 2.5–15 seconds
- Hourly threshold slider from $20–$40
- 10 banner color choices
- white/black text selection

Banner opacity is fixed at 80% and is not a setting.

## Build on GitHub
The repository includes `.github/workflows/build-apk.yml` plus a visible root-level backup `build-apk.yml`.

After uploading the project to a GitHub repository, the workflow builds:
`app/build/outputs/apk/debug/app-debug.apk`

The GitHub artifact is named:
`RideRate-v4-APK`
