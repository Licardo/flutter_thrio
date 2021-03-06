//
//  NavigatorNavigationController.m
//  thrio
//
//  Created by aadan on 2020/11/24.
//

#import "NavigatorFlutterEngineFactory.h"
#import "NavigatorFlutterViewController.h"
#import "NavigatorNavigationController.h"
#import "ThrioNavigator+PageBuilders.h"
#import "ThrioTypes.h"
#import "UIViewController+Internal.h"
#import "UIViewController+Navigator.h"

NS_ASSUME_NONNULL_BEGIN

@interface NavigatorNavigationController ()

@property (nonatomic) NSString *initialUrl;
@property (nonatomic, nullable) id initialParams;

@end

@implementation NavigatorNavigationController

- (instancetype)initWithUrl:(NSString *)url params:(id _Nullable)params {
    _initialUrl = url;
    _initialParams = params;
    UIViewController *viewController;
    NavigatorPageBuilder builder = [ThrioNavigator pageBuilders][url];
    if (builder) {
        viewController = builder(params);
        if (viewController.thrio_hidesNavigationBar_ == nil) {
            viewController.thrio_hidesNavigationBar_ = @NO;
        }
    }
    if (!viewController) {
        NSString *entrypoint = @"main";
        if (NavigatorFlutterEngineFactory.shared.multiEngineEnabled) {
            entrypoint = [url componentsSeparatedByString:@"/"][1];
        }
        NavigatorFlutterPageBuilder flutterBuilder = [ThrioNavigator flutterPageBuilder];
        if (flutterBuilder) {
            viewController = flutterBuilder(entrypoint);
        } else {
            viewController = [[NavigatorFlutterViewController alloc] initWithEntrypoint:entrypoint];
        }
    }
    if ([viewController isKindOfClass:NavigatorFlutterViewController.class]) {
        NavigatorFlutterViewController *fvc = (NavigatorFlutterViewController *)viewController;
        __weak typeof(self) weakSelf = self;
        [fvc setFlutterViewDidRenderCallback:^{
            __strong typeof(weakSelf) strongSelf = weakSelf;
            [strongSelf.topViewController thrio_pushUrl:strongSelf.initialUrl
                                                  index:@1
                                                 params:strongSelf.initialParams
                                               animated:NO
                                         fromEntrypoint:nil
                                                 result:nil
                                           poppedResult:nil];
        }];
    }
    return [super initWithRootViewController:viewController];
}

@end

NS_ASSUME_NONNULL_END
