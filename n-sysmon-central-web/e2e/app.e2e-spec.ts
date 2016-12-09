import { NSysmonCentralPage } from './app.po';

describe('n-sysmon-central App', function() {
  let page: NSysmonCentralPage;

  beforeEach(() => {
    page = new NSysmonCentralPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
