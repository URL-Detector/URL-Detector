package com.linkedin.urls.detection;

/** Represents a list of UrlDetectorOptions to allow multiple config options to be set in a single UrlDetector instance.
 * @author Jem Jensen (jemjensen)
 * @since 0.1.24
 */
public class UrlDetectorOptionsList {
  /**
   * Constructor - instantiates the list using a UrlDetectorOptionsListBuilder
   * @param builder The UrlDetectorOptionsListBuilder object
   */
  public UrlDetectorOptionsList(UrlDetectorOptionsListBuilder builder) {
    this._optionsList = builder._optionsList;
  }

  /**
   * Returns the list of options
   * @return An array of UrlDetectorOptions
   */
  public UrlDetectorOptions[] getOptions() {
    return _optionsList.clone();
  }

  /**
   * Implements the Builder pattern for easier readability and easier setting of multiple config options
   */
  public static class UrlDetectorOptionsListBuilder {
    private UrlDetectorOptions[] _optionsList;

    /**
     * Constructor - instantiates the list with a single "Default" option
     */
    public UrlDetectorOptionsListBuilder() {
      this._optionsList = new UrlDetectorOptions[1];
      _optionsList[0] = UrlDetectorOptions.Default;
    }

    /**
     * Adds a single option to the existing list of options.
     * @param options A single UrlDetectorOptions option
     * @return Returns a UrlDetectorOptionsListBuilder containing the new option
     */
    public UrlDetectorOptionsListBuilder addOption(UrlDetectorOptions options) {
      UrlDetectorOptions[] oldOptList = _optionsList;
      UrlDetectorOptions[] newOptList = new UrlDetectorOptions[oldOptList.length+1];
      int cur = 0;
      for (UrlDetectorOptions opt:oldOptList) {
        newOptList[cur] = oldOptList[cur];
        cur++;
      }
      newOptList[cur] = options;
      this._optionsList = newOptList;
      return this;
    }

    /**
     * Builds a UrlDetectorOptionsList from the UrlDetectorOptionsListBuilder.
     * @return A UrlDetectorOptionsList containing all options previously set in the builder.
     */
    public UrlDetectorOptionsList build() {
      return new UrlDetectorOptionsList(this);
    }
  }

  /**
   * Checks each option in the list to see if the flag is set
   * @param flag The UrlDetectorOptions flag we're looking for
   * @return Returns true if the option is set, otherwise false
   */
  public boolean hasFlag(UrlDetectorOptions flag) {
    for (UrlDetectorOptions opt:getOptions()) {
      if (opt.hasFlag(flag)) {
        return true;
      }
    }
    return false;
  }

  private UrlDetectorOptions[] _optionsList;
}
