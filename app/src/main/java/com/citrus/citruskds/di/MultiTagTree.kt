package com.citrus.citruskds.di

import timber.log.Timber

class MultiTagTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String? {
//        return "[${super.createStackElementTag(element)}][${element.methodName}]" //[className][functionName]
        return "[${element.methodName}][${super.createStackElementTag(element)}]" // [functionName][className]
    }
}