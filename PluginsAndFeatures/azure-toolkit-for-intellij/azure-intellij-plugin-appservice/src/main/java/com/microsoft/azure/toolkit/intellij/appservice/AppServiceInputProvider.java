package com.microsoft.azure.toolkit.intellij.appservice;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInputProvider;
import com.microsoft.azure.toolkit.intellij.appservice.input.AppServiceNameInput;

import javax.annotation.Nonnull;

//public class AppServiceInputProvider implements GuidanceInputProvider {
//    @Override
//    public GuidanceInput createInputComponent(@Nonnull InputConfig config, @Nonnull Context context) {
//        final ComponentContext inputContext = new ComponentContext(config, context);
//        if (config.getName().equals("input.appservice.name")) {
//            return new AppServiceNameInput(config, inputContext);
//        }
//        return null;
//    }
//}
