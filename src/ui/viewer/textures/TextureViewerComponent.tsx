import { Button, Upload } from "antd";
import { UploadChangeParam, UploadFile } from "antd/lib/upload/interface";
import { observer } from "mobx-react";
import React, { Component, ReactNode } from "react";
import { get_texture_renderer } from "../../../rendering/TextureRenderer";
import { texture_viewer_store } from "../../../stores/TextureViewerStore";
import { RendererComponent } from "../../RendererComponent";
import "./TextureViewerComponent.less";
import { AutoSizer } from "react-virtualized";

export class TextureViewerComponent extends Component {
    render(): ReactNode {
        return (
            <section className="v-t-TextureViewerComponent">
                <Toolbar />
                <div className="v-t-TextureViewerComponent-renderer">
                    <AutoSizer>
                        {({ width, height }) => (
                            <RendererComponent
                                renderer={get_texture_renderer()}
                                width={width}
                                height={height}
                            />
                        )}
                    </AutoSizer>
                </div>
            </section>
        );
    }
}

@observer
class Toolbar extends Component {
    state = {
        filename: undefined,
    };

    render(): ReactNode {
        return (
            <div className="v-t-TextureViewerComponent-toolbar">
                <Upload
                    accept=".xvm"
                    showUploadList={false}
                    onChange={this.load_file}
                    // Make sure it doesn't do a POST:
                    customRequest={() => false}
                >
                    <Button icon="file">{this.state.filename || "Open file..."}</Button>
                </Upload>
            </div>
        );
    }

    private load_file = (info: UploadChangeParam<UploadFile>) => {
        if (info.file.originFileObj) {
            this.setState({ filename: info.file.name });
            texture_viewer_store.load_file(info.file.originFileObj as File);
        }
    };
}
