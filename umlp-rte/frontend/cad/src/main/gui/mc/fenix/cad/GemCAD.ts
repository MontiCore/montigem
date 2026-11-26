/* (c) https://github.com/MontiCore/monticore */

import { Component, OnInit, OnDestroy, ViewChild, ElementRef, OnChanges, SimpleChanges } from '@angular/core';
import { GemCADComponent, config } from '../../../../../../build/gui2java2ts/mc/fenix/cad/GemCADComponent';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { Marker } from '../../../../../../build/gui2java2ts/mc/fenix/cad/gemcadtypes/Marker';

@Component({
  standalone: true,
  imports: [...config.imports],
  selector: config.selector,
  templateUrl: config.templateUrl,
  styleUrls: config.styleUrls
})
export class GemCAD extends GemCADComponent implements OnInit, OnDestroy, OnChanges {

  @ViewChild('canvasContainer', { static: true }) canvasContainer!: ElementRef;

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private controls!: OrbitControls;
  private model!: THREE.Object3D | null;
  private raycaster = new THREE.Raycaster();
  private mousePosition = new THREE.Vector2();

  private popup!: HTMLElement;
  private loadingIndicator = document.createElement("div");
  private resizeObserver!: ResizeObserver;

  private highlightedObject: THREE.Object3D | null = null;
  private originalEmissiveColors = new Map<string, { color: THREE.Color, intensity: number }>();

  /**
   * The markers as plain array. The generated `markers` input also accepts an IUMLPList and converts it
   * asynchronously via setMarkers(), so the raw value in ngOnChanges must not be used directly.
   */
  private get internalMarkers(): Marker[] {
    return this.getMarkers() ?? [];
  }

  private get containerWidth(): number {
    return this.canvasContainer.nativeElement.clientWidth;
  }

  private get containerHeight(): number {
    return this.canvasContainer.nativeElement.clientHeight;
  }

  public override init(): void {
    super.init();
    this.initScene();
    this.animate();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['filePath'] && this.filePath) {
      this.loadModel(this.filePath);
    }

    if (changes['markers']) {
      this.unhighlightObject();
      if (this.popup) this.popup.style.display = 'none';
    }
  }

  initScene(): void {
    this.loadingIndicator.innerHTML = "Loading...";
    this.loadingIndicator.style.position = "absolute";
    this.loadingIndicator.style.top = "50%";
    this.loadingIndicator.style.left = "50%";
    this.loadingIndicator.style.transform = "translate(-50%, -50%)";
    this.loadingIndicator.style.padding = "10px";
    this.loadingIndicator.style.background = "rgba(255,255,255,0.8)";
    this.canvasContainer.nativeElement.appendChild(this.loadingIndicator);

    this.scene = new THREE.Scene();
    this.scene.background = new THREE.Color(0xffffff);

    this.renderer = new THREE.WebGLRenderer({ antialias: true });
    this.renderer.setSize(this.containerWidth, this.containerHeight);
    this.renderer.shadowMap.enabled = true;

    this.camera = new THREE.PerspectiveCamera(75, this.containerWidth / this.containerHeight, 0.1, 1000);
    this.camera.position.set(10, 0, -3);
    this.camera.up.set(0, 0, -1);

    this.controls = new OrbitControls(this.camera, this.renderer.domElement);
    this.controls.enableDamping = true;
    this.controls.minPolarAngle = Math.PI / 2;
    this.controls.maxPolarAngle = Math.PI / 2;
    this.controls.screenSpacePanning = false;
    this.controls.enableZoom = true;

    const keyLight = new THREE.DirectionalLight(0xffffff, 20);
    keyLight.position.set(5, 5, -5);
    this.scene.add(keyLight);
    const ambientLight = new THREE.AmbientLight(0xffffff, 1);
    this.scene.add(ambientLight);

    this.canvasContainer.nativeElement.appendChild(this.renderer.domElement);

    this.popup = document.createElement('div');
    this.popup.style.position = 'absolute';
    this.popup.style.backgroundColor = 'white';
    this.popup.style.padding = '5px 10px';
    this.popup.style.border = '1px solid black';
    this.popup.style.borderRadius = '4px';
    this.popup.style.pointerEvents = 'none';
    this.popup.style.zIndex = '1000';
    this.popup.style.display = 'none';
    document.body.appendChild(this.popup);

    window.addEventListener('click', this.onMouseClick.bind(this));
    window.addEventListener('mousemove', this.onMouseMove.bind(this));

    this.resizeObserver = new ResizeObserver(() => this.onResize());
    this.resizeObserver.observe(this.canvasContainer.nativeElement);
  }

  private loadModel(path: string): void {
    if (this.model) { this.scene.remove(this.model); this.model = null; }
    this.unhighlightObject();

    const loader = new GLTFLoader();
    loader.load(path, (gltf) => {
      this.model = gltf.scene;
      this.scene.add(this.model);
      if (this.loadingIndicator.parentNode) this.loadingIndicator.parentNode.removeChild(this.loadingIndicator);
    }, undefined, (e) => console.error(e));
  }

  private onMouseClick(event: MouseEvent): void {
    if (!this.model) {
      return;
    }

    const rect = this.renderer.domElement.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right ||
      event.clientY < rect.top || event.clientY > rect.bottom) {
      return;
    }

    this.mousePosition.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    this.mousePosition.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    this.raycaster.setFromCamera(this.mousePosition, this.camera);
    const intersects = this.raycaster.intersectObjects(this.model.children, true);

    if (intersects.length > 0) {
      let clickedObject = intersects[0].object;
      let componentNode: THREE.Object3D | null = clickedObject;

      while (componentNode && componentNode.parent && componentNode.parent !== this.model) {
        componentNode = componentNode.parent;
      }

      if (componentNode && componentNode.name) {
        const nodeName = componentNode.name;

        const foundMarker = this.internalMarkers.find(marker => marker._nodeName === nodeName);

        if (foundMarker) {
          if (this.getOnNodeClick()) {
            this.getOnNodeClick()(nodeName);

            this.popup.style.display = 'none';
          }
        }
      }
    }
  }

  private onMouseMove(event: MouseEvent): void {
    if (!this.model) return;

    const rect = this.renderer.domElement.getBoundingClientRect();
    this.mousePosition.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    this.mousePosition.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    this.raycaster.setFromCamera(this.mousePosition, this.camera);
    const intersects = this.raycaster.intersectObjects(this.model.children, true);

    let targetNode: THREE.Object3D | null = null;
    let targetMarker: Marker | undefined = undefined;

    if (intersects.length > 0) {
      let hoveredObject = intersects[0].object;
      let componentNode: THREE.Object3D | null = hoveredObject;

      while (componentNode && componentNode.parent && componentNode.parent !== this.model) {
        componentNode = componentNode.parent;
      }

      if (componentNode && componentNode.name) {
        targetMarker = this.internalMarkers.find(m => m._nodeName === componentNode!.name);
        if (targetMarker) {
          targetNode = componentNode;
        }
      }
    }

    if (this.highlightedObject !== targetNode) {
      this.unhighlightObject();
      if (targetNode) {
        this.highlightObject(targetNode);
        this.highlightedObject = targetNode;
      }
    }

    if (targetNode && targetMarker) {
      const labelText = targetMarker._label;

      if (labelText && labelText.trim().length > 0) {
        this.popup.innerText = labelText;
        this.popup.style.left = `${event.clientX + 15}px`;
        this.popup.style.top = `${event.clientY + 15}px`;
        this.popup.style.display = 'block';
      } else {
        this.popup.style.display = 'none';
      }
    } else {
      this.popup.style.display = 'none';
    }
  }

  private highlightObject(object: THREE.Object3D): void {
    if (!object) return;
    this.originalEmissiveColors.clear();
    object.traverse((child) => {
      if (child instanceof THREE.Mesh && child.material) {
        const materials = Array.isArray(child.material) ? child.material : [child.material];
        materials.forEach(material => {
          if (!this.originalEmissiveColors.has(material.uuid)) {
            this.originalEmissiveColors.set(material.uuid, {
              color: material.emissive.clone(),
              intensity: material.emissiveIntensity
            });
          }
          material.emissive.set(0xffffff);
          material.emissiveIntensity = 0.5;
        });
      }
    });
  }

  private unhighlightObject(): void {
    if (!this.highlightedObject) return;
    this.highlightedObject.traverse((child) => {
      if (child instanceof THREE.Mesh && child.material) {
        const materials = Array.isArray(child.material) ? child.material : [child.material];
        materials.forEach(material => {
          const originalState = this.originalEmissiveColors.get(material.uuid);
          if (originalState) {
            material.emissive.copy(originalState.color);
            material.emissiveIntensity = originalState.intensity;
          }
        });
      }
    });
    this.originalEmissiveColors.clear();
    this.highlightedObject = null;
  }

  private onResize(): void {
    if (!this.canvasContainer) return;
    this.renderer.setSize(this.containerWidth, this.containerHeight);
    this.camera.aspect = this.containerWidth / this.containerHeight;
    this.camera.updateProjectionMatrix();
  }

  private animate(): void {
    requestAnimationFrame(() => this.animate());
    this.controls.update();
    this.renderer.render(this.scene, this.camera);
  }

  public override destroy(): void {
    window.removeEventListener('click', this.onMouseClick.bind(this));
    window.removeEventListener('mousemove', this.onMouseMove.bind(this));
    if (this.resizeObserver) this.resizeObserver.disconnect();
    if (document.body.contains(this.popup)) document.body.removeChild(this.popup);
  }
}
