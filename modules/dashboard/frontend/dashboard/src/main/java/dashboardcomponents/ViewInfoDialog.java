package dashboardcomponents;

public class ViewInfoDialog extends ViewInfoDialogTOP {

  @Override
  public String getInfoText() {
    String info = this.getView() == null ? null : this.getView().getInfo();
    return info == null || info.isEmpty() ? "No info available for this view." : info;
  }
}
