/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

import { Component, Input, OnInit } from '@angular/core';

const thresholds = [
  { progress: 0.95, colors: ["gainsboro", "#fa4d56", "#da1e28"]},
  { progress: 0.85, colors: ["gainsboro", "#f1c21b", "#f1c21b"]},
  { progress: 0, colors: ["#24a148", "gainsboro", "gainsboro"]},
]

@Component({
    selector: 'app-admin-directory-entry-validity-period',
    templateUrl: './validity-period.component.html',
    styleUrls: ['./validity-period.component.scss'],
    standalone: false
})
export class ValidityPeriodComponent implements OnInit {
  @Input() notBefore = ""
  @Input() notAfter = ""
  @Input() compact = false
  text = ""

  threshold? = thresholds[2]

  progress = 0
  angle = 0

  constructor() { }

  ngOnInit(): void {
    const now = Date.now()
    const start = Date.parse(this.notBefore)
    const end = Date.parse(this.notAfter)
    this.progress = 1-(end-now)/(end-start)
    if (this.progress > 1) {
      this.progress = 1
    }
    this.angle = 360 * this.progress
    this.threshold = thresholds.find(t => this.progress >= t.progress)

    const daysLeft = (end-now)/1000/60/60/24
    if (daysLeft < 90) {
      this.text = "<3 Mon"
    } else if (daysLeft < 270) {
      this.text = "<9 Mon"
    } else {
      const yearsLeft = daysLeft / 365
      if (Math.floor(yearsLeft) > 1) {
        this.text = `>${Math.floor(yearsLeft)} Jahre`
      } else if (Math.floor(yearsLeft) == 1) {
        this.text = `>${Math.floor(yearsLeft)} Jahr`
      } else {
        this.text = `>${Math.floor(yearsLeft*12)} Mon`
      }
    }

  }

  get circleStyle() {

    const	background = `radial-gradient(white 45%, transparent 0%),
    conic-gradient(transparent 0deg ${this.angle}deg, ${this.threshold?.colors[0]} ${this.angle}deg 360deg),
    conic-gradient(${this.threshold?.colors[1]} 0deg, ${this.threshold?.colors[2]});`;

    return `--background:${background}`
  }

}
