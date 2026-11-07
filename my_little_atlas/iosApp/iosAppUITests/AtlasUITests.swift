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
        XCTAssertTrue(element("日子，一页一页").waitForExistence(timeout: 30))
        capture("01-journal-opening")
        tap("后一页 →")
        XCTAssertTrue(element("待出发清单").waitForExistence(timeout: 10))
        capture("02-journal-wishes")
        tap("后一页 →")
        XCTAssertTrue(element("到访回忆").waitForExistence(timeout: 10))
        capture("03-journal-memory")
        tap("收藏")
        XCTAssertTrue(element("我的收藏夹").waitForExistence(timeout: 10))
        capture("04-collections")
        tap("手账")
        XCTAssertTrue(element("到访回忆").waitForExistence(timeout: 10), "Notebook page should survive a tab switch")
        tap("我的")
        XCTAssertTrue(element("小满的城市手账").waitForExistence(timeout: 10))
        XCTAssertTrue(element("编辑资料").exists)
        capture("05-original-profile")
        tap("编辑资料")
        XCTAssertTrue(element("编辑个人资料").waitForExistence(timeout: 10))
        capture("06-edit-profile")
        tap("返回")
        app.swipeUp()
        tap("未完成的草稿")
        XCTAssertTrue(element("写到一半的到访会保存在这里。").waitForExistence(timeout: 10))
        capture("07-drafts")
    }
}
