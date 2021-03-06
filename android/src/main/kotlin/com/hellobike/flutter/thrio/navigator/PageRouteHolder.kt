/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Hellobike Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.hellobike.flutter.thrio.navigator

import android.app.Activity
import com.hellobike.flutter.thrio.BooleanCallback
import com.hellobike.flutter.thrio.NullableIntCallback
import io.flutter.embedding.android.ThrioActivity
import java.lang.ref.WeakReference

internal data class PageRouteHolder(val pageId: Int,
                                    val clazz: Class<out Activity>,
                                    val entrypoint: String = NAVIGATION_FLUTTER_ENTRYPOINT_DEFAULT) {
    var pushByThrio = false

    internal val routes by lazy { mutableListOf<PageRoute>() }

    var activity: WeakReference<out Activity>? = null

    fun hasRoute(url: String? = null, index: Int? = null): Boolean = when (url) {
        null -> routes.isNotEmpty()
        else -> routes.any {
            it.settings.url == url && (index == null || index == 0 || it.settings.index == index)
        }
    }

    fun lastRoute(url: String? = null, index: Int? = null): PageRoute? = when (url) {
        null -> routes.lastOrNull()
        else -> routes.lastOrNull {
            it.settings.url == url && (index == null || index == 0 || it.settings.index == index)
        }
    }

    fun lastRoute(entrypoint: String): PageRoute? = routes.lastOrNull { it.entrypoint == entrypoint }

    fun allRoute(url: String): List<PageRoute> = routes.takeWhile { it.settings.url == url }

    fun push(route: PageRoute, result: NullableIntCallback) {
        val activity = activity?.get()
        if (activity != null) {
            if (activity is ThrioActivity) {
                activity.onPush(route.settings.toArguments()) {
                    if (it) {
                        routes.add(route)
                        result(route.settings.index)
                    } else {
                        result(null)
                    }
                    PageRoutes.lastRoute = PageRoutes.lastRoute()
                }
            } else {
                routes.add(route)
                result(route.settings.index)
                RouteObservers.didPush(route.settings)
                PageRoutes.lastRoute = route
            }
        } else {
            result(null)
        }
    }

    fun notify(url: String, index: Int?, name: String, params: Any?, result: BooleanCallback) {
        var isMatch = false
        routes.forEach {
            if (it.settings.url == url
                    && (index == null || index == 0 || it.settings.index == index)) {
                isMatch = true
                it.addNotify(name, params)
            }
        }
        result(isMatch)
    }


    fun pop(params: Any?, animated: Boolean, result: BooleanCallback) {
        val lastRoute = lastRoute()
        if (lastRoute == null) {
            result(false)
            return
        }
        val activity = activity?.get()
        if (activity != null && !activity.isDestroyed) {
            if (activity is ThrioActivity) {
                lastRoute.settings.params = params
                lastRoute.settings.animated = animated
                activity.onPop(lastRoute.settings.toArguments()) { it ->
                    if (it) {
                        routes.remove(lastRoute)
                    }
                    result(it)
                    if (it) {
                        lastRoute.poppedResult?.invoke(params)
                        lastRoute.poppedResult = null
                        if (lastRoute.fromEntrypoint != NAVIGATION_NATIVE_ENTRYPOINT
                                && lastRoute.entrypoint != lastRoute.fromEntrypoint) {
                            FlutterEngineFactory.getEngine(lastRoute.fromEntrypoint)?.sendChannel?.onPop(lastRoute.settings.toArguments()) {}
                        }
                    }
                    PageRoutes.lastRoute = PageRoutes.lastRoute()
                }
            } else {
                routes.remove(lastRoute)
                result(true)
                lastRoute.poppedResult?.invoke(params)
                lastRoute.poppedResult = null
                if (lastRoute.fromEntrypoint != NAVIGATION_NATIVE_ENTRYPOINT) {
                    lastRoute.settings.params = params
                    lastRoute.settings.animated = animated
                    FlutterEngineFactory.getEngine(lastRoute.fromEntrypoint)?.sendChannel?.onPop(lastRoute.settings.toArguments()) {}
                }
                RouteObservers.didPop(lastRoute.settings)
                PageRoutes.lastRoute = PageRoutes.lastRoute()
            }
        } else {
            result(false)
            lastRoute.poppedResult = null
        }
    }

    fun popTo(url: String, index: Int?, animated: Boolean, result: BooleanCallback) {
        val route = lastRoute(url, index)
        if (route == null) {
            result(false)
            return
        }

        route.settings.animated = animated

        val activity = activity?.get()
        if (activity != null) {
            if (activity is ThrioActivity) {
                activity.onPopTo(route.settings.toArguments()) {
                    if (it) {
                        val lastIndex = routes.indexOf(route)
                        for (i in routes.size - 1 downTo lastIndex + 1) {
                            routes.removeAt(i)
                        }
                    }
                    result(it)
                    PageRoutes.lastRoute = PageRoutes.lastRoute()
                }
            } else {
                result(true)
                RouteObservers.didPopTo(route.settings)
                PageRoutes.lastRoute = route;
            }
        } else {
            result(false)
        }
    }

    fun remove(url: String, index: Int?, animated: Boolean, result: BooleanCallback) {
        val route = lastRoute(url, index)
        if (route == null) {
            result(false)
            return
        }

        route.settings.animated = animated

        val activity = activity?.get()
        if (activity != null) {
            if (activity is ThrioActivity) {
                activity.onRemove(route.settings.toArguments()) {
                    if (it) {
                        routes.remove(route)
                    }
                    result(it)
                    PageRoutes.lastRoute = PageRoutes.lastRoute()
                }
            } else {
                routes.remove(route)
                result(true)
                RouteObservers.didRemove(route.settings)
                PageRoutes.lastRoute = PageRoutes.lastRoute()
            }
        } else {
            result(false)
        }
    }

    fun didPop(routeSettings: RouteSettings) {
        routes.lastOrNull()?.let {
            if (it.settings == routeSettings) {
                routes.remove(it)
                PageRoutes.lastRoute = PageRoutes.lastRoute()
            }
        }
    }

    companion object {
        private const val TAG = "PageRouteHolder"
    }
}