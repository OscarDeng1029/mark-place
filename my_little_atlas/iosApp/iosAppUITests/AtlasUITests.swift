import XCTest

final class AtlasUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchEnvironment["ATLAS_UI_TEST_DEMO"] = "1"
        app.launch()
    }

    private func element(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@ OR identifier == %@", label, label)).firstMatch
    }
    private func tap(_ label: String) {
        let target = element(label)
        XCTAssertTrue(target.waitForExistence(timeout: 20), "Missing \(label)\n\(app.debugDescription)")
        target.tap()
    }
    private func capture(_ name: String) {
        let image = XCTAttachment(screenshot: app.screenshot())
        image.name = name
        image.lifetime = .keepAlways
        add(image)
    }

    func testSharedJournalAndOriginalProfile() {
        XCTAssertTrue(element("我的手账").waitForExistence(timeout: 30))
        XCTAssertTrue(element("第 1 / 3 页").exists)
        capture("01-journal-home")
        tap("后一页 →")
        XCTAssertTrue(element("第 2 / 3 页").waitForExistence(timeout: 10))
        capture("02-journal-second-page")
        tap("目录")
        XCTAssertTrue(element("手账目录").waitForExistence(timeout: 10))
        capture("03-directory-cities")
        tap("杭州  1")
        XCTAssertTrue(element("2026 年 10 月").exists)
        capture("04-directory-hangzhou")
        tap("街角花园")
        XCTAssertTrue(element("第 2 / 3 页").waitForExistence(timeout: 10))
        tap("收藏")
        XCTAssertTrue(element("我的收藏").waitForExistence(timeout: 10))
        tap("南京  1")
        XCTAssertTrue(element("一颗柠檬").exists)
        capture("05-collections-cities")
        tap("手账")
        XCTAssertTrue(element("第 2 / 3 页").waitForExistence(timeout: 10), "Notebook page should survive a tab switch")
        tap("添加")
        XCTAssertTrue(element("今天，想记下什么？").waitForExistence(timeout: 10))
        capture("06-simple-add")
        tap("写手账")
        XCTAssertTrue(element("可以直接填写新地点，不用先收藏。").waitForExistence(timeout: 10))
        capture("07-direct-entry")
        tap("取消")
        tap("我的")
        XCTAssertTrue(element("小满的城市手账").waitForExistence(timeout: 10))
        XCTAssertTrue(element("编辑资料").exists)
        capture("08-original-profile")
        tap("编辑资料")
        XCTAssertTrue(element("编辑个人资料").waitForExistence(timeout: 10))
        capture("09-edit-profile")
        tap("返回")
        app.swipeUp()
        tap("未完成的草稿")
        XCTAssertTrue(element("写到一半的到访会保存在这里。").waitForExistence(timeout: 10))
        capture("10-drafts")
    }
}
