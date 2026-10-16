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

import { Injectable } from '@angular/core';

export enum MessageType {
    info, error, success, warning
}

export interface Message {
    type: MessageType
    text: string
}

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  messages: Message[] = [];

  info(text: string) {
    this.add({type: MessageType.info, text: text})
  }

  error(text: string) {
    this.add({type: MessageType.error, text: text})
  }

  success(text: string) {
    this.add({type: MessageType.success, text: text})
  }

  warning(text: string) {
    this.add({type: MessageType.warning, text: text})
  }

  add(message: Message) {
    this.messages.push(message);
  }

  remove (message: Message) {
    this.messages = this.messages.filter(obj => obj !== message);
  }

  clear() {
    this.messages = [];
  }
}