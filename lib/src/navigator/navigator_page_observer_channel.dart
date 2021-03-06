// The MIT License (MIT)
//
// Copyright (c) 2019 Hellobike Group
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

import 'package:flutter/widgets.dart';

import '../channel/thrio_channel.dart';
import 'navigator_page_observer.dart';
import 'navigator_page_observers.dart';
import 'navigator_page_route.dart';
import 'navigator_route_settings.dart';

typedef NavigatorPageObserverCallback = void Function(
  NavigatorPageObserver observer,
  RouteSettings settings,
);

class NavigatorPageObserverChannel {
  NavigatorPageObserverChannel(this._observers, String entrypoint)
      : _channel = ThrioChannel(channel: '__thrio_page_channel__$entrypoint') {
    _on(
      'willAppear',
      (observer, routeSettings) => observer.willAppear(routeSettings),
    );
    _on(
      'didAppear',
      (observer, routeSettings) => observer.didAppear(routeSettings),
    );
    _on(
      'willDisappear',
      (observer, routeSettings) => observer.willDisappear(routeSettings),
    );
    _on(
      'didDisappear',
      (observer, routeSettings) => observer.didDisappear(routeSettings),
    );
  }

  final ThrioChannel _channel;
  final NavigatorPageObservers _observers;

  void willAppear(
    RouteSettings routeSettings,
    NavigatorRouteAction routeAction,
  ) {
    final arguments = routeSettings.toArguments();
    arguments['routeAction'] = routeAction.toString().split('.').last;
    _channel.invokeMethod(
      'willAppear',
      arguments,
    );
  }

  void didAppear(
    RouteSettings routeSettings,
    NavigatorRouteAction routeAction,
  ) {
    final arguments = routeSettings.toArguments();
    arguments['routeAction'] = routeAction.toString().split('.').last;
    _channel.invokeMethod(
      'didAppear',
      arguments,
    );
  }

  void willDisappear(
    RouteSettings routeSettings,
    NavigatorRouteAction routeAction,
  ) {
    final arguments = routeSettings.toArguments();
    arguments['routeAction'] = routeAction.toString().split('.').last;
    _channel.invokeMethod(
      'willDisappear',
      arguments,
    );
  }

  void didDisappear(
    RouteSettings routeSettings,
    NavigatorRouteAction routeAction,
  ) {
    final arguments = routeSettings.toArguments();
    arguments['routeAction'] = routeAction.toString().split('.').last;
    _channel.invokeMethod(
      'didDisappear',
      arguments,
    );
  }

  void _on(String method, NavigatorPageObserverCallback callback) =>
      _channel.registryMethodCall(method, ([arguments]) {
        final routeSettings = NavigatorRouteSettings.fromArguments(arguments);
        final observers = _observers.observers;
        for (final observer in observers) {
          callback(observer, routeSettings);
        }
        return Future.value();
      });
}
